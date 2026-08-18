/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Adapted for an NDK application build from AOSP system/core libcutils
 * properties.cpp, trace-dev.cpp, and trace-dev.inc.
 */

#include <sys/system_properties.h>

#include <algorithm>
#include <atomic>
#include <cerrno>
#include <cinttypes>
#include <cstdarg>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fcntl.h>
#include <limits>
#include <pthread.h>
#include <unistd.h>

namespace {

constexpr uint64_t ATRACE_TAG_ALWAYS = 1ULL << 0;
constexpr uint64_t ATRACE_TAG_LAST = 1ULL << 27;
constexpr uint64_t ATRACE_TAG_VALID_MASK = (ATRACE_TAG_LAST - 1) | ATRACE_TAG_LAST;
constexpr uint64_t ATRACE_TAG_NOT_READY = 1ULL << 63;
constexpr size_t ATRACE_MESSAGE_LENGTH = 1024;

std::atomic_bool tracingEnabled = true;
pthread_once_t traceInitControl = PTHREAD_ONCE_INIT;
pthread_mutex_t traceTagsMutex = PTHREAD_MUTEX_INITIALIZER;

template <typename T>
T getIntegerProperty(const char* key, T defaultValue);

uint64_t readEnabledTraceTags();
void initializeTrace();
void writeTraceMessage(const char* format, ...);

struct PropertyCallbackData {
    void (*callback)(const char* name, const char* value, void* cookie);
    void* cookie;
};

void propertyCallback(void* rawData, const char* name, const char* value, unsigned int) {
    auto* data = static_cast<PropertyCallbackData*>(rawData);
    data->callback(name, value, data->cookie);
}

void propertyListCallback(const prop_info* info, void* rawData) {
    __system_property_read_callback(info, propertyCallback, rawData);
}

} // namespace

extern "C" {

std::atomic_bool atrace_is_ready = false;
int atrace_marker_fd = -1;
uint64_t atrace_enabled_tags = ATRACE_TAG_NOT_READY;

int property_get(const char* key, char* value, const char* defaultValue) {
    const int length = __system_property_get(key, value);
    if (length < 1 && defaultValue != nullptr) {
        std::snprintf(value, PROP_VALUE_MAX, "%s", defaultValue);
        return static_cast<int>(std::strlen(value));
    }
    return length;
}

int8_t property_get_bool(const char* key, int8_t defaultValue) {
    if (key == nullptr) return defaultValue;

    char value[PROP_VALUE_MAX] = {};
    const int length = property_get(key, value, "");
    if (length == 1) {
        if (value[0] == '0' || value[0] == 'n') return false;
        if (value[0] == '1' || value[0] == 'y') return true;
    } else if (length > 1) {
        if (
            std::strcmp(value, "no") == 0 ||
            std::strcmp(value, "false") == 0 ||
            std::strcmp(value, "off") == 0
        ) {
            return false;
        }
        if (
            std::strcmp(value, "yes") == 0 ||
            std::strcmp(value, "true") == 0 ||
            std::strcmp(value, "on") == 0
        ) {
            return true;
        }
    }
    return defaultValue;
}

int64_t property_get_int64(const char* key, int64_t defaultValue) {
    return getIntegerProperty(key, defaultValue);
}

int32_t property_get_int32(const char* key, int32_t defaultValue) {
    return getIntegerProperty(key, defaultValue);
}

int property_set(const char* key, const char* value) {
    return __system_property_set(key, value);
}

int property_list(
    void (*callback)(const char* name, const char* value, void* cookie),
    void* cookie
) {
    PropertyCallbackData data = {callback, cookie};
    return __system_property_foreach(propertyListCallback, &data);
}

void atrace_init() {
    pthread_once(&traceInitControl, initializeTrace);
}

void atrace_setup() {
    atrace_init();
}

uint64_t atrace_get_enabled_tags() {
    atrace_init();
    return atrace_enabled_tags;
}

void atrace_update_tags() {
    const uint64_t tags = tracingEnabled.load(std::memory_order_acquire) && atrace_marker_fd >= 0
        ? readEnabledTraceTags()
        : 0;
    pthread_mutex_lock(&traceTagsMutex);
    atrace_enabled_tags = tags;
    pthread_mutex_unlock(&traceTagsMutex);
}

void atrace_set_tracing_enabled(bool enabled) {
    tracingEnabled.store(enabled, std::memory_order_release);
    atrace_update_tags();
}

void atrace_begin_body(const char* name) {
    writeTraceMessage("B|%d|%s", getpid(), name);
}

void atrace_end_body() {
    writeTraceMessage("E|%d", getpid());
}

void atrace_async_begin_body(const char* name, int32_t cookie) {
    writeTraceMessage("S|%d|%s|%" PRId32, getpid(), name, cookie);
}

void atrace_async_end_body(const char* name, int32_t cookie) {
    writeTraceMessage("F|%d|%s|%" PRId32, getpid(), name, cookie);
}

void atrace_async_for_track_begin_body(const char* trackName, const char* name, int32_t cookie) {
    writeTraceMessage("G|%d|%s|%s|%" PRId32, getpid(), trackName, name, cookie);
}

void atrace_async_for_track_end_body(const char* trackName, int32_t cookie) {
    writeTraceMessage("H|%d|%s|%" PRId32, getpid(), trackName, cookie);
}

void atrace_instant_body(const char* name) {
    writeTraceMessage("I|%d|%s", getpid(), name);
}

void atrace_instant_for_track_body(const char* trackName, const char* name) {
    writeTraceMessage("N|%d|%s|%s", getpid(), trackName, name);
}

void atrace_int_body(const char* name, int32_t value) {
    writeTraceMessage("C|%d|%s|%" PRId32, getpid(), name, value);
}

void atrace_int64_body(const char* name, int64_t value) {
    writeTraceMessage("C|%d|%s|%" PRId64, getpid(), name, value);
}

} // extern "C"

namespace {

template <typename T>
T getIntegerProperty(const char* key, T defaultValue) {
    if (key == nullptr) return defaultValue;

    char value[PROP_VALUE_MAX] = {};
    if (property_get(key, value, "") < 1) return defaultValue;

    const int savedErrno = errno;
    errno = 0;
    char* end = nullptr;
    const intmax_t parsed = std::strtoimax(value, &end, 0);
    const bool valid = errno != ERANGE && end != value &&
        parsed >= std::numeric_limits<T>::min() &&
        parsed <= std::numeric_limits<T>::max();
    errno = savedErrno;
    return valid ? static_cast<T>(parsed) : defaultValue;
}

uint64_t readEnabledTraceTags() {
    char value[PROP_VALUE_MAX] = {};
    property_get("debug.atrace.tags.enableflags", value, "0");
    errno = 0;
    char* end = nullptr;
    const uint64_t tags = std::strtoull(value, &end, 0);
    if (errno == ERANGE || end == value || *end != '\0') return 0;
    return (tags | ATRACE_TAG_ALWAYS) & ATRACE_TAG_VALID_MASK;
}

void initializeTrace() {
    atrace_marker_fd = open("/sys/kernel/tracing/trace_marker", O_WRONLY | O_CLOEXEC);
    if (atrace_marker_fd < 0) {
        atrace_marker_fd = open("/sys/kernel/debug/tracing/trace_marker", O_WRONLY | O_CLOEXEC);
    }
    atrace_update_tags();
}

void writeTraceMessage(const char* format, ...) {
    if (atrace_marker_fd < 0) return;

    char message[ATRACE_MESSAGE_LENGTH];
    va_list arguments;
    va_start(arguments, format);
    const int length = std::vsnprintf(message, sizeof(message), format, arguments);
    va_end(arguments);
    if (length <= 0) return;

    const size_t bytes = std::min(static_cast<size_t>(length), sizeof(message) - 1);
    static_cast<void>(write(atrace_marker_fd, message, bytes));
}

} // namespace
