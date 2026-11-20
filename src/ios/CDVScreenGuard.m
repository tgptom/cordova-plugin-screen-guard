#import "CDVScreenGuard.h"
#import <UIKit/UIKit.h>

// iOS cannot reliably block screenshots. The private "secure UITextField"
// layer trick is deliberately NOT used: it can black out the whole WebView.
// Instead we notify JS so the app can hide sensitive content (e.g. an OTP).
// A screenshot taken while the content is visible still succeeds; the
// "screenshot" event only arrives afterwards.

@interface CDVScreenGuard ()
@property (nonatomic, assign) BOOL enabled;
@property (nonatomic, copy) NSString *eventCallbackId;
@end

@implementation CDVScreenGuard

- (void)pluginInitialize
{
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc addObserver:self selector:@selector(onWillResignActive:)
               name:UIApplicationWillResignActiveNotification object:nil];
    [nc addObserver:self selector:@selector(onUserDidTakeScreenshot:)
               name:UIApplicationUserDidTakeScreenshotNotification object:nil];
    [nc addObserver:self selector:@selector(onDidBecomeActive:)
               name:UIApplicationDidBecomeActiveNotification object:nil];
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)onReset
{
    // Page navigation/reload: the old JS callback is gone, and the new page must opt in again.
    self.eventCallbackId = nil;
    self.enabled = NO;
}

- (void)setSecure:(CDVInvokedUrlCommand *)command
{
    id arg = [command argumentAtIndex:0];
    CDVPluginResult *result;
    if (![arg isKindOfClass:[NSNumber class]]) {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                               messageAsDictionary:@{ @"code": @"INVALID_ARGUMENT",
                                                      @"message": @"enabled must be a boolean" }];
    } else {
        self.enabled = [(NSNumber *)arg boolValue];
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)watch:(CDVInvokedUrlCommand *)command
{
    self.eventCallbackId = command.callbackId;
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)onWillResignActive:(NSNotification *)note
{
    [self fire:@"resignActive"];
}

- (void)onUserDidTakeScreenshot:(NSNotification *)note
{
    [self fire:@"screenshot"];
}

- (void)onDidBecomeActive:(NSNotification *)note
{
    [self fire:@"becomeActive"];
}

- (void)fire:(NSString *)reason
{
    if (!self.enabled || self.eventCallbackId == nil) {
        return;
    }
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                            messageAsDictionary:@{ @"reason": reason }];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:self.eventCallbackId];
}

@end
