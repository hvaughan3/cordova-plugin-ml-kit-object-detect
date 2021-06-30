#import <Cordova/CDV.h>
@import MLKitObjectDetection;
@import MLKitObjectDetectionCommon;

@interface MlObjectDetect : CDVPlugin

@property CDVInvokedUrlCommand* commandglo;
// @property GMVDetector* textDetector;
@property UIImage* image;

- (void) detectObject:(CDVInvokedUrlCommand*)command;
- (UIImage *)resizeImage:(UIImage *)image;
- (NSData *)retrieveAssetDataPhotosFramework:(NSURL *)urlMedia;

@end
