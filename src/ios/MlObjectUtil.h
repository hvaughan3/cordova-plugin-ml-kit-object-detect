#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface MlObjectUtil : NSObject

+ (UIImageOrientation)imageOrientation;
+ (UIImageOrientation)imageOrientationFromDevicePosition:(AVCaptureDevicePosition)devicePosition;
+ (UIDeviceOrientation)currentUIOrientation;

@end

NS_ASSUME_NONNULL_END
