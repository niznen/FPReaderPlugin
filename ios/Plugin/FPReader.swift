import Foundation

@objc public class FPReader: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
