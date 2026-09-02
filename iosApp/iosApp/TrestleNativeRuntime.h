#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^TrestleRuntimeStarted)(void);
typedef void (^TrestleRuntimeOutput)(NSString *line);
typedef void (^TrestleRuntimeCompletion)(int exitCode, NSString *_Nullable errorMessage);

@interface TrestleNativeRuntime : NSObject

+ (BOOL)isAvailableWithReason:(NSString *_Nullable *_Nullable)reason
    NS_SWIFT_NAME(isAvailable(reason:));
+ (void)launchArguments:(NSArray<NSString *> *)arguments
        workingDirectory:(NSString *)workingDirectory
             environment:(NSDictionary<NSString *, NSString *> *)environment
                 started:(TrestleRuntimeStarted)started
                  output:(TrestleRuntimeOutput)output
              completion:(TrestleRuntimeCompletion)completion
    NS_SWIFT_NAME(launch(arguments:workingDirectory:environment:started:output:completion:));
+ (void)cancel;

@end

NS_ASSUME_NONNULL_END
