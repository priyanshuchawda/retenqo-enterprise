# Media ladder (Fedora checks 2026-08-25)

## Proven
- **Qwen3-VL-2B** on Uber receipt image → structured JSON (~2 min CPU).
- **WhatsApp media path exists:** `/sdcard/Android/media/com.whatsapp/WhatsApp/Media/{WhatsApp Images,WhatsApp Documents}/`
- **PDF ladder:** WA certificate PDF → `pdftoppm` → Tesseract OCR → clean text (name, course, score 95%, date). No VLM needed for digital PDFs.

## Implication for Mneme
Watch WA Media folders after download; OCR first; VL only if OCR weak. Notifications for text triage only.
