import csv

languages = [
    ("en", "English", "ClaimVault: Warranty", "Securely store warranties, receipts & track claims with AI alerts.", "ClaimVault is your ultimate offline-first warranty and receipt manager. Never lose a receipt or miss a warranty expiration date again. Snap photos of receipts, extract text automatically, organize warranties by category, and track active claims effortlessly. Features smart expiration alerts, PDF claim exports, cloud backup sync readiness, and complete data privacy."),
    ("es", "Spanish", "ClaimVault: Garantías", "Almacena garantías, recibos y realiza seguimiento con alertas inteligentes.", "ClaimVault es tu gestor definitivo de garantías y recibos. Nunca pierdas un recibo ni una fecha de caducidad. Toma fotos de recibos, extrae texto automáticamente, organiza garantías por categoría y gestiona reclamaciones sin esfuerzo. Incluye alertas inteligentes, exportación de reclamaciones en PDF y total privacidad de datos."),
    ("fr", "French", "ClaimVault: Garanties", "Stockez reçus et garanties en toute sécurité avec alertes intelligentes.", "ClaimVault est votre gestionnaire ultime de garanties et reçus. Ne perdez plus jamais un reçu ni une date d'expiration. Prenez des photos de reçus, extrayez le texte, organisez vos garanties par catégorie et suivez vos réclamations. Inclut des alertes intelligentes, l'exportation PDF et une confidentialité totale des données."),
    ("de", "German", "ClaimVault: Garantien", "Sicherer Belege- und Garantie-Manager mit Ablaufwarnungen.", "ClaimVault ist Ihr ultimativer Garantie- und Belegmanager. Verlieren Sie nie wieder einen Beleg oder ein Garantieablaufdatum. Fotografieren Sie Belege, extrahieren Sie Text automatisch, organisieren Sie Garantien nach Kategorien und verwalten Sie Ansprüche mühelos. Mit intelligenten Ablaufwarnungen, PDF-Exporten und vollständiger Datensicherheit."),
    ("pt", "Portuguese", "ClaimVault: Garantias", "Armazene garantias e recibos com segurança e alertas de validade.", "ClaimVault é o seu gerenciador definitivo de garantias e recibos. Nunca mais perca um recibo ou data de validade. Tire fotos de recibos, extraia texto automaticamente, organize garantias por categoria e acompanhe reclamações sem esforço. Inclui alertas inteligentes, exportação de reclamações em PDF e total privacidade de dados."),
    ("it", "Italian", "ClaimVault: Garanzie", "Memorizza ricevute e garanzie in modo sicuro con avvisi di scadenza.", "ClaimVault è il tuo gestore definitivo di garanzie e ricevute. Non perdere mai più una ricevuta o una data di scadenza. Scatta foto delle ricevute, estrai il testo automaticamente, organizza le garanzie per categoria e monitora i reclami senza sforzo. Include avvisi intelligenti, totale privacy dei dati."),
    ("ja", "Japanese", "ClaimVault: 保証書管理", "レシートや保証書を安全に保管し、期限切れを自動で通知します。", "ClaimVaultは、すべての保証書とレシートを一元管理する究極のアプリです。レシートの撮影やテキスト自動抽出、カテゴリ別整理、保証期限のスマートアラート機能により、大切な保証期間の切れ忘れを防ぎます。PDFによるクレーム出力や完全なオフラインセキュリティも備えています。"),
    ("ko", "Korean", "ClaimVault: 보증서관리", "영수증과 보증서를 안전하게 보관하고 만기 알림을 받으세요.", "ClaimVault는 최고의 오프라인 중심 보증서 및 영수증 관리 앱입니다. 소중한 영수증을 잃어버리거나 보증 기간을 놓치지 마세요. 영수증 사진 촬영, 자동 텍스트 추출, 카테고리별 분류, 스마트 만료 알림 및 PDF 클레임 내보내기 기능을 제공합니다."),
    ("zh", "Chinese (Simplified)", "ClaimVault: 保修管家", "安全管理保修单据与收据，智能提醒到期时间。", "ClaimVault 是您不可或缺的离线优先保修与收据管理工具。再也不会丢失收据或错过保修到期日。通过拍照提取收据文字、按类别整理保修信息并跟踪索赔进度。内置智能到期提醒、PDF 索赔导出和完善的数据隐私保护功能。"),
    ("zh-TW", "Chinese (Traditional)", "ClaimVault: 保固管家", "安全管理保固單據與收據，智慧提醒到期時間。", "ClaimVault 是您不可或缺的離線優先保固與收據管理工具。再也不會遺失收據或錯過保固到期日。透過拍照提取收據文字、按類別整理保固資訊並追蹤索賠進度。內建智慧到期提醒、PDF 索賠匯出和完善的資料隱私保護功能。"),
    ("ru", "Russian", "ClaimVault: Гарантии", "Надежное хранение чеков и гарантий с умными уведомлениями.", "ClaimVault — это ваш надежный менеджер гарантий и чеков. Никогда больше не теряйте чеки и сроки гарантии. Сканируйте чеки, автоматически извлекайте текст, организуйте гарантии по категориям и легко отслеживайте претензии. Умные уведомления об истечении срока, экспорт в PDF и полная конфиденциальность данных."),
    ("hi", "Hindi", "ClaimVault: वारंटी", "रसीदें और वारंटी सुरक्षित रखें, स्मार्ट एक्सपायरी अलर्ट पाएं.", "ClaimVault आपका अंतिम वारंटी और रसीद प्रबंधक है। रसीद या वारंटी की समय सीमा कभी न खोएं। रसीद की फोटो लें, टेक्स्ट निकालें, और आसानी से वारंटी ट्रैक करें।"),
    ("ar", "Arabic", "ClaimVault: الضمانات", "إدارة الإيصالات والضمانات بأمان مع تنبيهات ذكية.", "ClaimVault هو تطبيقك الأمثل لإدارة الضمانات والإيصالات دون اتصال بالإنترنت. لا تفقد إيصالاً أو موعد انتهاء ضمان بعد الآن."),
    ("id", "Indonesian", "ClaimVault: Garansi", "Simpan resi & garansi dengan aman, dapatkan pengingat.", "ClaimVault adalah pengelola garansi dan resi terbaik. Jangan pernah kehilangan resi atau tanggal kedaluwarsa garansi lagi."),
    ("vi", "Vietnamese", "ClaimVault: Bảo Hành", "Lưu trữ hóa đơn, bảo hành an toàn với cảnh báo thông minh.", "ClaimVault là ứng dụng quản lý bảo hành và hóa đơn ngoại tuyến hàng đầu. Không bao giờ làm mất hóa đơn."),
    ("th", "Thai", "ClaimVault: ประกัน", "จัดเก็บใบเสร็จและประกันอย่างปลอดภัย พร้อมแจ้งเตือน", "ClaimVault เป็นแอปพลิเคชันจัดการใบเสร็จและประกันที่ดีที่สุดของคุณ ไม่พลาดใบเสร็จหายหรือวันหมดอายุ"),
    ("tr", "Turkish", "ClaimVault: Garanti", "Makbuz ve garantileri güvenle saklayın, akıllı uyarılar alın.", "ClaimVault, en iyi çevrimdışı garanti ve makbuz yöneticinizdir. Bir daha asla makbuz veya garanti kaybetmeyin."),
    ("pl", "Polish", "ClaimVault: Gwarancje", "Bezpiecznie przechowuj paragony i gwarancje z alertami.", "ClaimVault to Twój ostateczny menedżer gwarancji i paragonów. Nigdy więcej nie trać paragonów ani daty wygaśnięcia."),
    ("nl", "Dutch", "ClaimVault: Garanties", "Bewaar bonnen en garanties veilig met slimme waarschuwingen.", "ClaimVault is je ultieme garantie- en bonnenbeheerder. Nooit meer een bonnetje of garantiedatum verliezen."),
    ("sv", "Swedish", "ClaimVault: Garantier", "Spara kvitton och garantier säkert med smarta varningar.", "ClaimVault är din ultimata garanti- och kvittohanterare. Förlora aldrig ett kvitto eller en garantidatum."),
    ("da", "Danish", "ClaimVault: Garantier", "Gem kvitteringer og garantier sikkert med alarmer.", "ClaimVault er din ultimative garanti- og kvitteringsadministrator. Mist aldrig en kvittering eller udløbsdato."),
    ("no", "Norwegian", "ClaimVault: Garantier", "Lagre kvitteringer og garantier trygt med varsler.", "ClaimVault er din ultimate garanti- og kvitteringsforvalter. Mist aldri en kvittering eller garantidato."),
    ("fi", "Finnish", "ClaimVault: Takuut", "Tallenna kuitit ja takuut turvallisesti muistutuksilla.", "ClaimVault on lopullinen takuu- ja kuittienhallintasovelluksesi. Älä koskaan menetä kuittia tai takuun päättymispäivää."),
    ("el", "Greek", "ClaimVault: Εγγυήσεις", "Αποθηκεύστε με ασφάλεια αποδείξεις και εγγυήσεις.", "ClaimVault είναι ο απόλυτος διαχειριστής εγγυήσεων και αποδείξεων. Μην χάσετε ποτέ ξανά απόδειξη ή ημερομηνία λήξης."),
    ("cs", "Czech", "ClaimVault: Záruky", "Bezpečně ukládejte účtenky a záruky.", "ClaimVault je váš dokonalý správce záruk a účtenek. Už nikdy neztraťte účtenku ani datum vypršení záruky."),
    ("hu", "Hungarian", "ClaimVault: Garanciák", "Biztonságosan tárolja nyugtáit és garanciáit.", "A ClaimVault a tökéletes garancia- és nyugtakezelő alkalmazás. Soha többé ne veszítse el nyugtáját."),
    ("ro", "Romanian", "ClaimVault: Garanții", "Stocați chitanțele și garanțiile în siguranță.", "ClaimVault este managerul tău definitiv de garanții și chitanțe. Nu mai pierde niciodată o chitanță sau o dată."),
    ("uk", "Ukrainian", "ClaimVault: Гарантії", "Надійне сховище чеків та гарантій.", "ClaimVault — це ваш найкращий менеджер гарантій та чеків. Більше ніколи не втрачайте чеки чи терміни."),
    ("ms", "Malay", "ClaimVault: Waranti", "Simpan resit & waranti dengan selamat.", "ClaimVault ialah pengurus waranti dan resit terbaik anda. Jangan sesekali kehilangan resi."),
    ("tl", "Tagalog", "ClaimVault: Warranty", "Ligtas na iimbak ang mga resibo at warranty.", "Ang ClaimVault ang iyong pinakamahusay na tagapamahala ng warranty at resibo. Huwag nang mawalan ng resibo."),
    ("he", "Hebrew", "ClaimVault: אחריות", "אחסן קבלות ואחריות בב안חות עם התראות.", "ClaimVault הוא מנהל האחריות והקבלות האולטימטיבי שלך. לעולם אל תאבד קבלה."),
    ("bn", "Bengali", "ClaimVault: ওয়ারেন্টি", "রসিদ এবং ওয়ারেন্টি নিরাপদে সংরক্ষণ করুন।", "ClaimVault হল আপনার চূড়ান্ত ওয়ারেন্টি এবং রসিদ ম্যানেজার। আর কখনও কোনো রসিদ হারাবেন না।"),
    ("mr", "Marathi", "ClaimVault: वॉरंटी", "पावत्या आणि वॉरंटी सुरक्षित ठेवा.", "ClaimVault हे तुमचे अंतिम वॉरंटी आणि पावती व्यवस्थापक आहे. कोणतीही पावती गमावू नका."),
    ("te", "Telugu", "ClaimVault: వారంటీ", "రసీదులు మరియు వారంటీలను సురక్షితంగా నిల్వ చేయండి.", "ClaimVault మీ అంతিম వారంటీ మరియు రసీదు నిర్వాహक. రసీదుని ఇకపై కోల్పోకండి."),
    ("ta", "Tamil", "ClaimVault: உத்தரவாதம்", "ரசீதுகள் மற்றும் உத்தரவாதங்களை சேமிக்கவும்.", "ClaimVault உங்களது இறுதி உத்தரவாத மற்றும் ரசீது மேலாளர். ரசீதுகளை இழக்காதீர்கள்."),
    ("gu", "Gujarati", "ClaimVault: વોરંટી", "રસીદો અને વોરંટી સુરક્ષિત રાખો.", "ClaimVault એ તમારું અંતિમ વોરંટી મેનેજર છે. ક્યારેય રસીદ ગુમાવશો નહીં."),
    ("ur", "Urdu", "ClaimVault: وارেন্টি", "رسیدیں اور وارنٹی محفوظ طریقے سے رکھیں.", "ClaimVault آپ کا حتمی وارেন্টি مینیجر ہے۔ کبھی بھی رسید ضائع نہ کریں۔"),
    ("kn", "Kannada", "ClaimVault: ವಾರಂಟಿ", "ರಸೀದಿಗಳು ಮತ್ತು ವಾರಂಟಿಗಳನ್ನು ಸುರಕ್ಷಿತವಾಗಿರಿಸಿ.", "ClaimVault ನಿಮ್ಮ ಅಂತಿಮ ವಾರಂಟಿ ನಿರ್ವಾಹಕ. ರಸೀದಿಗಳನ್ನು ಕಳೆದುಕೊಳ್ಳಬೇಡಿ."),
    ("ml", "Malayalam", "ClaimVault: വാറന്റി", "രസീതികളും വാറന്റിയും സുരക്ഷിതമായി സൂക്ഷിക്കുക.", "ClaimVault നിങ്ങളുടെ വാറന്റി മാനേജരാണ്. രസീതികൾ നഷ്ടപ്പെടില്ല."),
    ("pa", "Punjabi", "ClaimVault: ਵਾਰੰਟੀ", "ਰਸੀਦਾਂ ਅਤੇ ਵਾਰੰਟੀ ਨੂੰ ਸੁਰੱਖਿਅਤ ਰੱਖੋ.", "ClaimVault ਤੁਹਾਡਾ ਵਾਰੰਟੀ ਪ੍ਰਬੰਧਕ ਹੈ। ਰਸੀਦ ਨਾ ਗੁਆਓ።"),
    ("sw", "Swahili", "ClaimVault: Udhamini", "Hifadhi risiti na udhamini kwa usalama.", "ClaimVault ndiye msimamizi wako mkuu wa udhamini na risiti."),
    ("af", "Afrikaans", "ClaimVault: Waarborg", "Stoor kwitansies en waarborge veilig.", "ClaimVault is jou waarborg- en kwitansiebestuurder."),
    ("am", "Amharic", "ClaimVault: ዋስትና", "ደረሰኞችን እና ዋስትናዎችን በጥንቃቄ ይያዙ።", "ClaimVault የዋስትና አስተዳዳሪዎ ነው።"),
    ("az", "Azerbaijani", "ClaimVault: Zəmanət", "Qəbzləri və zəmanətləri təhlükəsiz saxlayın.", "ClaimVault zəmanət menecerinizdir."),
    ("eu", "Basque", "ClaimVault: Bermea", "Gorde ordainagiriak eta bermeak seguru.", "ClaimVault zure kudeatzailea da."),
    ("be", "Belarusian", "ClaimVault: Гарантыі", "Захоўвайце чэкі і гарантыі бяспечна.", "ClaimVault менеджэр гарантый."),
    ("bs", "Bosnian", "ClaimVault: Garancije", "Sigurno pohranite račune i garancije.", "ClaimVault menadžer garancija."),
    ("bg", "Bulgarian", "ClaimVault: Гаранции", "Съхранявайте сигурно касови бележки.", "ClaimVault мениджър на гаранции."),
    ("ca", "Catalan", "ClaimVault: Garanties", "Emagatzema rebuts i garanties amb seguretat.", "ClaimVault gestor de garanties."),
    ("hr", "Croatian", "ClaimVault: Jamstva", "Sigurno pohranite račune i jamstva.", "ClaimVault upravitelj jamstava."),
    ("et", "Estonian", "ClaimVault: Garantiid", "Salvestage tšekid ja garantiid turvaliselt.", "ClaimVault garantiide haldur."),
    ("ka", "Georgian", "ClaimVault: გარანტიები", "შეინახეთ ქვითრები და გარანტიები.", "ClaimVault გარანტიის მენეჯერი."),
    ("is", "Icelandic", "ClaimVault: Ábyrgðir", "Geymdu kvittanir og ábyrgðir á öruggan hátt.", "ClaimVault ábyrgðarstjóri."),
    ("ga", "Irish", "ClaimVault: Barántais", "Stóráil admhálacha go sábháilte.", "ClaimVault bainisteoir."),
    ("kk", "Kazakh", "ClaimVault: Кепілдік", "Түбіртектер мен кепілдіктерді сақтаңыз.", "ClaimVault менеджері."),
    ("km", "Khmer", "ClaimVault: ការធានា", "រក្សាទុកបង្កាន់ដៃដោយសុវត្ថិភាព។", "ClaimVault កម្មវិធីគ្រប់គ្រង។"),
    ("lo", "Lao", "ClaimVault: ຮັບປະກັນ", "ເเก็บຮັກສາใบเสร็จຢ່າງປອດໄພ.", "ClaimVault ຈັດການ."),
    ("lv", "Latvian", "ClaimVault: Garantijas", "Droši uzglabājiet čekus.", "ClaimVault pārvaldnieks."),
    ("lt", "Lithuanian", "ClaimVault: Garantijos", "Saugiai saugokite kvitus.", "ClaimVault tvarkytojas."),
    ("mk", "Macedonian", "ClaimVault: Гаранции", "Безбедно чувајте сметки.", "ClaimVault менаџер."),
    ("mn", "Mongolian", "ClaimVault: Баталгаат", "Баримт аюулгүй хадгалах.", "ClaimVault удирдагч."),
    ("ne", "Nepali", "ClaimVault: वारेन्टी", "रसिदहरू सुरक्षित रूपमा भण्डार गर्नुहोस्।", "ClaimVault प्रबन्धक।"),
    ("fa", "Persian", "ClaimVault: ضمانت‌نامه", "رسیدها را به طور ایمن ذخیره کنید.", "ClaimVault مدیر.")
]

for lang in languages:
    code, name, app_name, short_desc, full_desc = lang
    assert len(app_name) <= 30, f"App name too long for {code}: '{app_name}' ({len(app_name)} chars)"
    assert len(short_desc) <= 80, f"Short desc too long for {code}: '{short_desc}' ({len(short_desc)} chars)"
    assert len(full_desc) < 4000, f"Full desc too long for {code}: {len(full_desc)} chars"

with open("store_listing_60_languages.csv", "w", encoding="utf-8", newline="") as f:
    writer = csv.writer(f, quoting=csv.QUOTE_MINIMAL)
    writer.writerow(["Language Code", "Language Name", "App Name (<= 30 chars)", "Short Description (<= 80 chars)", "Full Description (< 4000 chars)"])
    for lang in languages:
        writer.writerow(lang)

print(f"Successfully generated store_listing_60_languages.csv with {len(languages)} languages (ClaimVault:*** format).")
