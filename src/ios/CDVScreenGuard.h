#import <Cordova/CDVPlugin.h>

@interface CDVScreenGuard : CDVPlugin

- (void)setSecure:(CDVInvokedUrlCommand *)command;
- (void)watch:(CDVInvokedUrlCommand *)command;

@end
