#import "TrestleNativeRuntime.h"

#import <TargetConditionals.h>
#import <QuartzCore/CAMetalLayer.h>
#import <UIKit/UIKit.h>
#import <dlfcn.h>
#import <signal.h>
#import <stdatomic.h>
#import <stdint.h>
#import <stdlib.h>
#import <string.h>
#import <sys/mman.h>
#import <unistd.h>

typedef int JliLaunchFunction(
    int argc,
    const char **argv,
    int jargc,
    const char **jargv,
    int appclassc,
    const char **appclassv,
    const char *fullversion,
    const char *dotversion,
    const char *pname,
    const char *lname,
    unsigned char javaargs,
    unsigned char cpwildcard,
    unsigned char javaw,
    int ergo
);

static atomic_bool TrestleRuntimeCancelled = false;

#define TRESTLE_EXPORT __attribute__((used, visibility("default")))

typedef void *EGLDisplay;
typedef void *EGLConfig;
typedef void *EGLContext;
typedef void *EGLSurface;
typedef int EGLBoolean;
typedef int EGLint;

enum {
    TrestleEglFalse = 0,
    TrestleEglNone = 0x3038,
    TrestleEglRedSize = 0x3024,
    TrestleEglGreenSize = 0x3023,
    TrestleEglBlueSize = 0x3022,
    TrestleEglAlphaSize = 0x3021,
    TrestleEglDepthSize = 0x3025,
    TrestleEglSurfaceType = 0x3033,
    TrestleEglWindowBit = 0x0004,
    TrestleEglRenderableType = 0x3040,
    TrestleEglOpenGles3Bit = 0x0040,
    TrestleEglOpenGlesApi = 0x30A0,
    TrestleEglContextClientVersion = 0x3098,
};

typedef EGLDisplay (*EglGetDisplay)(void *display);
typedef EGLBoolean (*EglInitialize)(EGLDisplay display, EGLint *major, EGLint *minor);
typedef EGLBoolean (*EglChooseConfig)(
    EGLDisplay display,
    const EGLint *attributes,
    EGLConfig *config,
    EGLint configSize,
    EGLint *configCount
);
typedef EGLBoolean (*EglBindApi)(EGLint api);
typedef EGLSurface (*EglCreateWindowSurface)(
    EGLDisplay display,
    EGLConfig config,
    void *window,
    const EGLint *attributes
);
typedef EGLContext (*EglCreateContext)(
    EGLDisplay display,
    EGLConfig config,
    EGLContext shared,
    const EGLint *attributes
);
typedef EGLBoolean (*EglMakeCurrent)(
    EGLDisplay display,
    EGLSurface draw,
    EGLSurface read,
    EGLContext context
);
typedef EGLBoolean (*EglSwapBuffers)(EGLDisplay display, EGLSurface surface);
typedef EGLBoolean (*EglSwapInterval)(EGLDisplay display, EGLint interval);
typedef EGLBoolean (*EglDestroySurface)(EGLDisplay display, EGLSurface surface);
typedef EGLBoolean (*EglDestroyContext)(EGLDisplay display, EGLContext context);
typedef EGLBoolean (*EglTerminate)(EGLDisplay display);

typedef struct {
    EGLContext context;
    EGLSurface surface;
} TrestleEglWindow;

static struct {
    void *library;
    EGLDisplay display;
    EGLConfig config;
    EglGetDisplay getDisplay;
    EglInitialize initialize;
    EglChooseConfig chooseConfig;
    EglBindApi bindApi;
    EglCreateWindowSurface createWindowSurface;
    EglCreateContext createContext;
    EglMakeCurrent makeCurrent;
    EglSwapBuffers swapBuffers;
    EglSwapInterval swapInterval;
    EglDestroySurface destroySurface;
    EglDestroyContext destroyContext;
    EglTerminate terminate;
} TrestleEgl;

static TrestleEglWindow *TrestleCurrentWindow = NULL;
static int64_t TrestleShowingWindow = 0;
static double TrestleCursorX = 0;
static double TrestleCursorY = 0;
static void (*TrestleCursorCallback)(void *, double, double) = NULL;
static void (*TrestleMouseCallback)(void *, int, int, int) = NULL;

@interface TrestleGameSurfaceView : UIView
@end

@implementation TrestleGameSurfaceView

+ (Class)layerClass {
    return CAMetalLayer.class;
}

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    UITouch *touch = touches.anyObject;
    CGPoint point = [touch locationInView:self];
    TrestleCursorX = point.x * self.contentScaleFactor;
    TrestleCursorY = point.y * self.contentScaleFactor;
    if (TrestleCursorCallback != NULL) {
        TrestleCursorCallback((void *)TrestleShowingWindow, TrestleCursorX, TrestleCursorY);
    }
    if (TrestleMouseCallback != NULL) {
        TrestleMouseCallback((void *)TrestleShowingWindow, 0, 1, 0);
    }
}

- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    UITouch *touch = touches.anyObject;
    CGPoint point = [touch locationInView:self];
    TrestleCursorX = point.x * self.contentScaleFactor;
    TrestleCursorY = point.y * self.contentScaleFactor;
    if (TrestleCursorCallback != NULL) {
        TrestleCursorCallback((void *)TrestleShowingWindow, TrestleCursorX, TrestleCursorY);
    }
}

- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    if (TrestleMouseCallback != NULL) {
        TrestleMouseCallback((void *)TrestleShowingWindow, 0, 0, 0);
    }
}

- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(UIEvent *)event {
    [self touchesEnded:touches withEvent:event];
}

@end

static TrestleGameSurfaceView *TrestleGameSurface = nil;

static void TrestleShowGameSurface(void) {
    dispatch_sync(dispatch_get_main_queue(), ^{
        UIWindowScene *scene = (UIWindowScene *)UIApplication.sharedApplication.connectedScenes.allObjects.firstObject;
        UIWindow *window = scene.windows.firstObject;
        if (window == nil) return;
        if (TrestleGameSurface == nil) {
            TrestleGameSurface = [[TrestleGameSurfaceView alloc] initWithFrame:window.bounds];
            TrestleGameSurface.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
            TrestleGameSurface.backgroundColor = UIColor.blackColor;
            TrestleGameSurface.contentScaleFactor = UIScreen.mainScreen.nativeScale;
            CAMetalLayer *layer = (CAMetalLayer *)TrestleGameSurface.layer;
            layer.contentsScale = TrestleGameSurface.contentScaleFactor;
            layer.framebufferOnly = NO;
            [window addSubview:TrestleGameSurface];
        }
    });
}

static void TrestleHideGameSurface(void) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [TrestleGameSurface removeFromSuperview];
        TrestleGameSurface = nil;
    });
}

static BOOL TrestleLoadEgl(void) {
    if (TrestleEgl.display != NULL) return YES;
    NSString *path = [NSBundle.mainBundle.bundlePath stringByAppendingPathComponent:@"Frameworks/libEGL.framework/libEGL"];
    TrestleEgl.library = dlopen(path.fileSystemRepresentation, RTLD_NOW | RTLD_GLOBAL);
    if (TrestleEgl.library == NULL) return NO;
#define TRESTLE_EGL_SYMBOL(field, name) TrestleEgl.field = (Egl##name)dlsym(TrestleEgl.library, "egl" #name)
    TRESTLE_EGL_SYMBOL(getDisplay, GetDisplay);
    TRESTLE_EGL_SYMBOL(initialize, Initialize);
    TRESTLE_EGL_SYMBOL(chooseConfig, ChooseConfig);
    TRESTLE_EGL_SYMBOL(bindApi, BindApi);
    TRESTLE_EGL_SYMBOL(createWindowSurface, CreateWindowSurface);
    TRESTLE_EGL_SYMBOL(createContext, CreateContext);
    TRESTLE_EGL_SYMBOL(makeCurrent, MakeCurrent);
    TRESTLE_EGL_SYMBOL(swapBuffers, SwapBuffers);
    TRESTLE_EGL_SYMBOL(swapInterval, SwapInterval);
    TRESTLE_EGL_SYMBOL(destroySurface, DestroySurface);
    TRESTLE_EGL_SYMBOL(destroyContext, DestroyContext);
    TRESTLE_EGL_SYMBOL(terminate, Terminate);
#undef TRESTLE_EGL_SYMBOL
    if (
        TrestleEgl.getDisplay == NULL || TrestleEgl.initialize == NULL || TrestleEgl.chooseConfig == NULL ||
        TrestleEgl.bindApi == NULL || TrestleEgl.createWindowSurface == NULL || TrestleEgl.createContext == NULL ||
        TrestleEgl.makeCurrent == NULL || TrestleEgl.swapBuffers == NULL
    ) return NO;
    TrestleEgl.display = TrestleEgl.getDisplay(NULL);
    if (TrestleEgl.display == NULL || !TrestleEgl.initialize(TrestleEgl.display, NULL, NULL)) return NO;
    const EGLint attributes[] = {
        TrestleEglRedSize, 8, TrestleEglGreenSize, 8, TrestleEglBlueSize, 8, TrestleEglAlphaSize, 8,
        TrestleEglDepthSize, 24, TrestleEglSurfaceType, TrestleEglWindowBit,
        TrestleEglRenderableType, TrestleEglOpenGles3Bit, TrestleEglNone,
    };
    EGLint count = 0;
    return TrestleEgl.bindApi(TrestleEglOpenGlesApi) &&
        TrestleEgl.chooseConfig(TrestleEgl.display, attributes, &TrestleEgl.config, 1, &count) && count > 0;
}

TRESTLE_EXPORT int pojavInit(int useStackQueue) {
    return 1;
}

TRESTLE_EXPORT void *pojavCreateContext(void *sharedPointer) {
    TrestleShowGameSurface();
    if (!TrestleLoadEgl()) return NULL;
    TrestleEglWindow *shared = sharedPointer;
    TrestleEglWindow *window = calloc(1, sizeof(TrestleEglWindow));
    const EGLint contextAttributes[] = {TrestleEglContextClientVersion, 3, TrestleEglNone};
    window->surface = TrestleEgl.createWindowSurface(
        TrestleEgl.display,
        TrestleEgl.config,
        (__bridge void *)TrestleGameSurface.layer,
        NULL
    );
    window->context = TrestleEgl.createContext(
        TrestleEgl.display,
        TrestleEgl.config,
        shared == NULL ? NULL : shared->context,
        contextAttributes
    );
    if (window->surface == NULL || window->context == NULL) {
        free(window);
        return NULL;
    }
    return window;
}

TRESTLE_EXPORT void *pojavGetCurrentContext(void) {
    return TrestleCurrentWindow;
}

TRESTLE_EXPORT void pojavMakeCurrent(TrestleEglWindow *window) {
    if (window == NULL) {
        TrestleEgl.makeCurrent(TrestleEgl.display, NULL, NULL, NULL);
        TrestleCurrentWindow = NULL;
        return;
    }
    if (TrestleEgl.makeCurrent(TrestleEgl.display, window->surface, window->surface, window->context)) {
        TrestleCurrentWindow = window;
    }
}

TRESTLE_EXPORT void pojavSwapBuffers(void) {
    if (TrestleCurrentWindow != NULL) {
        TrestleEgl.swapBuffers(TrestleEgl.display, TrestleCurrentWindow->surface);
    }
}

TRESTLE_EXPORT void pojavSwapInterval(int interval) {
    if (TrestleEgl.swapInterval != NULL) TrestleEgl.swapInterval(TrestleEgl.display, interval);
}

TRESTLE_EXPORT void pojavSetWindowHint(int hint, int value) {}
TRESTLE_EXPORT void pojavPumpEvents(void *window) {}
TRESTLE_EXPORT void pojavRewindEvents(void) {}

TRESTLE_EXPORT void pojavTerminate(void) {
    if (TrestleCurrentWindow != NULL) {
        if (TrestleEgl.destroySurface != NULL) {
            TrestleEgl.destroySurface(TrestleEgl.display, TrestleCurrentWindow->surface);
        }
        if (TrestleEgl.destroyContext != NULL) {
            TrestleEgl.destroyContext(TrestleEgl.display, TrestleCurrentWindow->context);
        }
        free(TrestleCurrentWindow);
        TrestleCurrentWindow = NULL;
    }
    if (TrestleEgl.terminate != NULL) TrestleEgl.terminate(TrestleEgl.display);
    TrestleHideGameSurface();
}

typedef int64_t TrestleJLong;
typedef int32_t TrestleJInt;
typedef uint8_t TrestleJBoolean;

#define TRESTLE_CALLBACK_SETTER(name, storage) \
    TRESTLE_EXPORT TrestleJLong Java_org_lwjgl_glfw_GLFW_nglfwSet##name##Callback( \
        void *environment, void *classObject, TrestleJLong window, TrestleJLong callback \
    ) { \
        void *previous = (void *)(uintptr_t)storage; \
        storage = (__typeof__(storage))(uintptr_t)callback; \
        return (TrestleJLong)(uintptr_t)previous; \
    }

static void *TrestleCharCallback;
static void *TrestleCharModsCallback;
static void *TrestleCursorEnterCallback;
static void *TrestleFramebufferCallback;
static void *TrestleKeyCallback;
static void *TrestleScrollCallback;
static void *TrestleWindowSizeCallback;

TRESTLE_CALLBACK_SETTER(Char, TrestleCharCallback)
TRESTLE_CALLBACK_SETTER(CharMods, TrestleCharModsCallback)
TRESTLE_CALLBACK_SETTER(CursorEnter, TrestleCursorEnterCallback)
TRESTLE_CALLBACK_SETTER(CursorPos, TrestleCursorCallback)
TRESTLE_CALLBACK_SETTER(FramebufferSize, TrestleFramebufferCallback)
TRESTLE_CALLBACK_SETTER(Key, TrestleKeyCallback)
TRESTLE_CALLBACK_SETTER(MouseButton, TrestleMouseCallback)
TRESTLE_CALLBACK_SETTER(Scroll, TrestleScrollCallback)
TRESTLE_CALLBACK_SETTER(WindowSize, TrestleWindowSizeCallback)

#undef TRESTLE_CALLBACK_SETTER

TRESTLE_EXPORT void Java_org_lwjgl_glfw_GLFW_nglfwSetShowingWindow(
    void *environment,
    void *classObject,
    TrestleJLong window
) {
    TrestleShowingWindow = window;
    if (window != 0) TrestleShowGameSurface();
}

TRESTLE_EXPORT void Java_org_lwjgl_glfw_GLFW_nglfwGetCursorPos(
    void *environment,
    void *classObject,
    TrestleJLong window,
    void *xBuffer,
    void *yBuffer
) {}

TRESTLE_EXPORT void Java_org_lwjgl_glfw_GLFW_nglfwGetCursorPosA(
    void *environment,
    void *classObject,
    TrestleJLong window,
    void *xArray,
    void *yArray
) {}

TRESTLE_EXPORT void Java_org_lwjgl_glfw_GLFW_glfwSetCursorPos(
    void *environment,
    void *classObject,
    TrestleJLong window,
    double x,
    double y
) {
    TrestleCursorX = x;
    TrestleCursorY = y;
}

TRESTLE_EXPORT void *Java_org_lwjgl_glfw_CallbackBridge_nativeClipboard(
    void *environment,
    void *classObject,
    TrestleJInt action,
    void *copyBytes
) {
    return NULL;
}

TRESTLE_EXPORT void Java_org_lwjgl_glfw_CallbackBridge_nativeSetGrabbing(
    void *environment,
    void *classObject,
    TrestleJBoolean grabbing
) {}

@implementation TrestleNativeRuntime

+ (BOOL)isAvailableWithReason:(NSString **)reason {
#if TARGET_OS_SIMULATOR
    if (reason != NULL) *reason = @"The bundled iOS game runtime supports physical ARM64 devices only.";
    return NO;
#else
    NSBundle *bundle = NSBundle.mainBundle;
    NSString *javaHome = [bundle.bundlePath stringByAppendingPathComponent:@"java_runtimes/java-25-openjdk"];
    NSString *jli = [javaHome stringByAppendingPathComponent:@"lib/libjli.dylib"];
    NSString *lwjgl = [bundle.bundlePath stringByAppendingPathComponent:@"Frameworks/liblwjgl.dylib"];
    if (![NSFileManager.defaultManager fileExistsAtPath:jli] ||
        ![NSFileManager.defaultManager fileExistsAtPath:lwjgl]) {
        if (reason != NULL) *reason = @"This app package does not contain the iOS Java and LWJGL runtime payload.";
        return NO;
    }
    void *jitProbe = mmap(
        NULL,
        (size_t)getpagesize(),
        PROT_READ | PROT_WRITE | PROT_EXEC,
        MAP_PRIVATE | MAP_ANON | MAP_JIT,
        -1,
        0
    );
    if (jitProbe == MAP_FAILED) {
        if (reason != NULL) *reason = @"The iOS runtime is bundled, but this process has no usable JIT permission.";
        return NO;
    }
    munmap(jitProbe, (size_t)getpagesize());
    return YES;
#endif
}

+ (void)launchArguments:(NSArray<NSString *> *)arguments
        workingDirectory:(NSString *)workingDirectory
             environment:(NSDictionary<NSString *, NSString *> *)environment
                 started:(TrestleRuntimeStarted)started
                  output:(TrestleRuntimeOutput)output
              completion:(TrestleRuntimeCompletion)completion {
    NSString *reason = nil;
    if (![self isAvailableWithReason:&reason]) {
        completion(-1, reason);
        return;
    }
    atomic_store(&TrestleRuntimeCancelled, false);
    dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED, 0), ^{
        @autoreleasepool {
            if (atomic_load(&TrestleRuntimeCancelled)) {
                completion(-1, @"The iOS game launch was cancelled.");
                return;
            }
            [environment enumerateKeysAndObjectsUsingBlock:^(NSString *key, NSString *value, BOOL *stop) {
                setenv(key.UTF8String, value.UTF8String, 1);
            }];
            setenv("HACK_IGNORE_START_ON_FIRST_THREAD", "1", 1);
            setenv("LIBGL_NOINTOVLHACK", "1", 1);
            setenv("LIBGL_NORMALIZE", "1", 1);
            setenv("POJAV_RENDERER", "libgl4es_114.dylib", 0);
            if (chdir(workingDirectory.fileSystemRepresentation) != 0) {
                completion(-1, @"The iOS game directory could not be opened.");
                return;
            }

            NSString *bundlePath = NSBundle.mainBundle.bundlePath;
            NSString *frameworks = [bundlePath stringByAppendingPathComponent:@"Frameworks"];
            NSArray<NSString *> *preloads = @[
                @"libMoltenVK.dylib",
                @"libglapi.0.dylib",
                @"libGLESv2.framework/libGLESv2",
                @"libEGL.framework/libEGL",
                @"libgl4es_114.dylib",
                @"libopenal.dylib",
                @"liblwjgl.dylib",
            ];
            for (NSString *library in preloads) {
                NSString *path = [frameworks stringByAppendingPathComponent:library];
                if ([NSFileManager.defaultManager fileExistsAtPath:path] && dlopen(path.fileSystemRepresentation, RTLD_NOW | RTLD_GLOBAL) == NULL) {
                    const char *error = dlerror();
                    completion(-1, [NSString stringWithFormat:@"Could not load %@: %s", library, error ?: "unknown error"]);
                    return;
                }
            }

            NSString *jliPath = [bundlePath stringByAppendingPathComponent:@"java_runtimes/java-25-openjdk/lib/libjli.dylib"];
            void *jliLibrary = dlopen(jliPath.fileSystemRepresentation, RTLD_NOW | RTLD_GLOBAL);
            if (jliLibrary == NULL) {
                const char *error = dlerror();
                completion(-1, [NSString stringWithFormat:@"Could not load Java: %s", error ?: "unknown error"]);
                return;
            }
            JliLaunchFunction *launch = (JliLaunchFunction *)dlsym(jliLibrary, "JLI_Launch");
            if (launch == NULL) {
                completion(-1, @"The bundled Java runtime has no JLI_Launch entry point.");
                return;
            }

            NSMutableArray<NSString *> *runtimeArguments = arguments.mutableCopy;
            CGRect nativeBounds = UIScreen.mainScreen.nativeBounds;
            NSString *windowSize = [NSString stringWithFormat:@"-Dglfw.windowSize=%dx%d", (int)nativeBounds.size.width, (int)nativeBounds.size.height];
            NSString *refreshRate = [NSString stringWithFormat:@"-DUIScreen.maximumFramesPerSecond=%d", (int)UIScreen.mainScreen.maximumFramesPerSecond];
            for (NSUInteger index = 0; index < runtimeArguments.count; index++) {
                if ([runtimeArguments[index] hasPrefix:@"-Dglfw.windowSize="]) runtimeArguments[index] = windowSize;
                if ([runtimeArguments[index] hasPrefix:@"-DUIScreen.maximumFramesPerSecond="]) runtimeArguments[index] = refreshRate;
            }
            NSUInteger count = runtimeArguments.count;
            const char **argv = calloc(count, sizeof(char *));
            if (argv == NULL) {
                completion(-1, @"The iOS JVM argument list could not be allocated.");
                return;
            }
            for (NSUInteger index = 0; index < count; index++) {
                argv[index] = strdup(runtimeArguments[index].UTF8String);
            }
            signal(SIGSEGV, SIG_DFL);
            signal(SIGPIPE, SIG_DFL);
            signal(SIGBUS, SIG_DFL);
            signal(SIGILL, SIG_DFL);
            signal(SIGFPE, SIG_DFL);
            started();
            output(@"Starting the bundled iOS Java runtime.");
            int exitCode = launch(
                (int)count, argv, 0, NULL, 0, NULL, "25-internal", "25", "java", "openjdk", 0, 1, 0, 1
            );
            for (NSUInteger index = 0; index < count; index++) free((void *)argv[index]);
            free(argv);
            completion(exitCode, nil);
        }
    });
}

+ (void)cancel {
    atomic_store(&TrestleRuntimeCancelled, true);
}

@end
