package com.example.util

fun String.localize(lang: AppLanguage): String {
    if (lang == AppLanguage.EN) return this
    return LocalizationMap.translate(this, lang)
}

object LocalizationMap {
    private val viMap = mapOf(
        // Onboarding
        "Welcome to ClaimVault" to "Chào mừng đến với ClaimVault",
        "100% Offline & Private" to "Ngoại tuyến & Riêng tư 100%",
        "Tired of paper receipts fading or warranties disappearing? Store, track, and protect everything in one fully local app. Zero cloud databases, zero ads, zero mandatory tracking." to "Mệt mỏi vì hóa đơn giấy bị phai màu hoặc giấy bảo hành bị biến mất? Lưu trữ, theo dõi và bảo vệ tất cả trong một ứng dụng hoàn toàn cục bộ. Không cơ sở dữ liệu đám mây, không quảng cáo, không theo dõi bắt buộc.",
        "Instant OCR Scan" to "Quét OCR tức thì",
        "Frictionless Auto-Fill" to "Tự động điền không ma sát",
        "Simply snap a photo of any purchase receipt. Our on-device offline scanner automatically detects the merchant name, purchase date, and total cost to populate your vault in seconds." to "Chỉ cần chụp ảnh hóa đơn mua hàng bất kỳ. Máy quét ngoại tuyến ngay trên thiết bị sẽ tự động phát hiện tên người bán, ngày mua và tổng chi phí để cập nhật kho của bạn chỉ trong vài giây.",
        "Expiry Alerts & PDF Export" to "Cảnh báo hết hạn & Xuất PDF",
        "Be Claim-Ready Instantly" to "Sẵn sàng khiếu nại tức thì",
        "Get notified with gentle push reminders before your warranties run out. Generate fully packaged PDF claims featuring your item details and high-resolution receipt in just one click." to "Nhận thông báo nhắc nhở nhẹ nhàng trước khi bảo hành hết hạn. Tạo hồ sơ khiếu nại PDF đầy đủ bao gồm thông tin sản phẩm và hóa đơn độ phân giải cao chỉ với một cú nhấp chuột.",
        "Skip" to "Bỏ qua",
        "Next" to "Tiếp theo",
        "Get Started" to "Bắt đầu",
        "ClaimVault" to "ClaimVault",

        // Settings Screen
        "Settings & Preferences" to "Cài đặt & Tùy chọn",
        "App Theme Mode" to "Chế độ giao diện",
        "Customize ClaimVault appearance or follow system system automatic dark theme." to "Tùy chỉnh giao diện ClaimVault hoặc theo chế độ tối tự động của hệ thống.",
        "System Auto" to "Tự động hệ thống",
        "Light" to "Sáng",
        "Dark" to "Tối",
        "COMMUNITY & FEEDBACK" to "CỘNG ĐỒNG & PHẢN HỒI",
        "Rate 5 Stars" to "Đánh giá 5 sao",
        "Share App" to "Chia sẻ ứng dụng",
        "Stored 100% Locally On Your Device" to "Lưu trữ 100% cục bộ trên thiết bị của bạn",
        "ClaimVault operates with zero server overhead, zero mandatory accounts, zero ads, and zero cloud tracking. Integrated with Firebase Free Tier analytics & remote configuration helpers." to "ClaimVault hoạt động với không chi phí máy chủ, không yêu cầu tài khoản, không quảng cáo và không theo dõi đám mây. Tích hợp với phân tích Firebase Free Tier & hỗ trợ cấu hình từ xa.",
        "ENCRYPTED LOCAL BACKUP" to "SAO LƯU CỤC BỘ MÃ HÓA",
        "Export your entire vault into an uncompressed JSON backup file to transfer to a new phone or archive safely." to "Xuất toàn bộ kho của bạn thành tệp sao lưu JSON không nén để chuyển sang điện thoại mới hoặc lưu trữ an toàn.",
        "Export Backup" to "Xuất bản sao lưu",
        "Restore Backup" to "Khôi phục bản sao lưu",
        "Encrypted Google Drive & Cloud Backup" to "Sao lưu đám mây & Google Drive mã hóa",
        "Export your vault into an AES-256 encrypted .enc file protected by your passphrase. You can securely store this file in your private Google Drive folder while maintaining 100% zero-knowledge offline privacy." to "Xuất kho của bạn thành tệp .enc được mã hóa AES-256 được bảo vệ bằng mật khẩu của bạn. Bạn có thể lưu trữ tệp này một cách an toàn trong thư mục Google Drive riêng tư của mình trong khi vẫn duy trì quyền riêng tư ngoại tuyến 100% zero-knowledge.",
        "Encryption Passphrase" to "Mật khẩu mã hóa",
        "Export .enc" to "Xuất .enc",
        "Restore .enc" to "Khôi phục .enc",
        "Smart Storage Saver" to "Tiết kiệm dung lượng thông minh",
        "High-resolution camera photos are automatically enhanced and compressed to < 300KB so you can store thousands of receipts without clogging phone memory." to "Ảnh chụp camera độ phân giải cao được tự động cải thiện và nén xuống dưới 300KB để bạn có thể lưu trữ hàng ngàn hóa đơn mà không làm nghẹt bộ nhớ điện thoại.",
        "Saved Receipts" to "Hóa đơn đã lưu",
        "Total Disk Space Used" to "Tổng dung lượng đã dùng",
        "ABOUT CLAIMVAULT" to "VỀ CLAIMVAULT",
        "Core Philosophy" to "Triết lý cốt lõi",
        "Never lose a receipt, never miss a warranty claim. No mandatory cloud accounts, no subscription paywalls, and zero ads." to "Không bao giờ mất hóa đơn, không bao giờ bỏ lỡ khiếu nại bảo hành. Không yêu cầu tài khoản đám mây, không thu phí thuê bao và không có quảng cáo.",

        // Analytics & Dashboard
        "Vault Analytics & Dashboard" to "Phân tích & Bảng điều khiển",
        "Total Protected" to "Tổng giá trị bảo vệ",
        "Active Items" to "Sản phẩm hoạt động",
        "Expiring Soon" to "Sắp hết hạn",
        "Total Vault Items" to "Tổng số sản phẩm",
        "Monthly Spending Trends" to "Xu hướng chi tiêu hàng tháng",
        "Purchase spending across the last 6 months" to "Chi tiêu mua sắm trong 6 tháng qua",
        "Items by Category" to "Sản phẩm theo danh mục",
        "No warranty items in vault yet." to "Chưa có sản phẩm bảo hành nào.",
        "Items" to "Sản phẩm",

        // Home Screen
        "SCAN NEW RECEIPT" to "QUÉT HÓA ĐƠN MỚI",
        "Search your vault..." to "Tìm kiếm kho lưu trữ...",
        "ACTIVE PROTECTION" to "BẢO VỆ ĐANG HOẠT ĐỘNG",
        "No items match your search." to "Không tìm thấy sản phẩm phù hợp.",
        "Securely protecting" to "Đang bảo vệ an toàn",
        "warranties" to "sản phẩm bảo hành",
        "All" to "Tất cả",
        "Electronics" to "Điện tử",
        "Appliances" to "Gia dụng",
        "Automotive" to "Ô tô xe máy",
        "Furniture" to "Nội thất",
        "Clothing" to "Quần áo",
        "Other" to "Khác",
        "EXPIRING SOON" to "SẮP HẾT HẠN",
        "Vault Analytics" to "Phân tích kho lưu trữ",
        "Back" to "Quay lại",
        "Scan" to "Quét",
        "Add" to "Thêm",
        "Edit" to "Sửa",
        "Delete" to "Xóa",
        "Settings" to "Cài đặt",
        "Select Language" to "Chọn ngôn ngữ",
        "App Language" to "Ngôn ngữ ứng dụng",
        "Change app display language instantly." to "Thay đổi ngôn ngữ hiển thị của ứng dụng tức thì.",

        // Add/Edit Screen
        "Quick Add Receipt" to "Thêm nhanh hóa đơn",
        "Edit Warranty" to "Sửa bảo hành",
        "Product Title" to "Tên sản phẩm",
        "Merchant / Store" to "Người bán / Cửa hàng",
        "Price Paid" to "Giá đã thanh toán",
        "Category" to "Danh mục",
        "Warranty Duration" to "Thời hạn bảo hành",
        "Serial Number" to "Số sê-ri",
        "Model Number" to "Số kiểu máy",
        "Notes & Coverage" to "Ghi chú & Phạm vi bảo hiểm",
        "Tags (comma separated)" to "Thẻ (phân tách bằng dấu phẩy)",
        "Purchase Date" to "Ngày mua",
        "Warranty Expiry" to "Ngày hết hạn",
        "Receipt / Invoice Image" to "Ảnh hóa đơn / Biên lai",
        "SELECT FROM GALLERY" to "CHỌN TỪ THƯ VIỆN",
        "TAKE PHOTO" to "CHỤP ẢNH MỚI",
        "SAVE ITEM" to "LƯU SẢN PHẨM",
        "Saving..." to "Đang lưu...",
        "Detected Text" to "Văn bản phát hiện được",

        // Detail Screen
        "Warranty Details" to "Chi tiết bảo hành",
        "Expired" to "Đã hết hạn",
        "Active" to "Đang hoạt động",
        "Purchase Details" to "Chi tiết mua hàng",
        "Purchase Price" to "Giá mua",
        "Purchase Date" to "Ngày mua",
        "Warranty Expiry" to "Bảo hành hết hạn",
        "Model/Serial Info" to "Thông tin kiểu máy/Sê-ri",
        "Model Number" to "Số kiểu máy",
        "Serial Number" to "Số sê-ri",
        "Warranty Duration" to "Thời hạn bảo hành",
        "Notes & Coverage Details" to "Chi tiết ghi chú & bảo hành",
        "Receipt Image Proof" to "Bằng chứng ảnh hóa đơn",
        "DOWNLOAD CLAIM PDF" to "TẢI PDF KHIẾU NẠI",
        "EDIT WARRANTY" to "SỬA BẢO HÀNH",
        "DELETE WARRANTY" to "XÓA BẢO HÀNH",
        "months" to "tháng",
        "Lifetime" to "Trọn đời"
    )

    fun translate(key: String, lang: AppLanguage): String {
        if (lang == AppLanguage.EN) return key
        return viMap[key] ?: key
    }
}
