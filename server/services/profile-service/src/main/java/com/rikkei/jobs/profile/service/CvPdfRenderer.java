package com.rikkei.jobs.profile.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.rikkei.jobs.profile.dto.CvDtos.ItemView;
import com.rikkei.jobs.profile.dto.CvDtos.SectionView;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import com.rikkei.jobs.profile.entity.CvSection;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * HS-04: dựng CV mẫu Rikkei thành PDF, tông đỏ trắng, tiêu đề tiếng Anh.
 * Dải đỏ đầu trang (họ tên, vị trí, giới thiệu); cột chính bên trái (kỹ năng, kinh nghiệm);
 * cột phụ nền hồng nhạt bên phải (ảnh, liên hệ, QR, học vấn, chứng chỉ, ngoại ngữ).
 * HTML tự sinh (XHTML hợp lệ), mọi dữ liệu người dùng đều được escape.
 */
@Slf4j
@Component
public class CvPdfRenderer {
    /** Liberation Sans từ openpdf-fonts-extra: có đủ dấu tiếng Việt và bản đậm; font Base14 của PDF thì không. */
    private static final String FONT_DIR = "/liberation/LiberationSans-";
    private static final String BRAND = "#B4232D";
    private static final String SOFT = "#FFF5F5";
    /** Phần nằm ở cột phụ bên phải; SUMMARY nằm ở dải đầu trang; còn lại ở cột chính. */
    private static final Set<CvSection.Type> SIDE = Set.of(CvSection.Type.EDUCATION, CvSection.Type.CERTIFICATES, CvSection.Type.LANGUAGES);
    private static final String TECH_PREFIX = "Công nghệ: ";
    /** shortcut: đọc điểm từ chuỗi detail do CvService.skillItem dựng; đổi định dạng ở đó thì sửa cả đây. */
    private static final Pattern SKILL_SCORE = Pattern.compile("(\\d+) điểm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final Clock clock;
    private final ProfileFileService files;

    public CvPdfRenderer(@Qualifier("skillProfileClock") Clock clock, ProfileFileService files) {
        this.clock = clock; this.files = files;
    }

    public byte[] render(CvService.Rendered cv) {
        var out = new ByteArrayOutputStream();
        try {
            var builder = new PdfRendererBuilder();
            builder.useFastMode();
            font(builder, "Regular", 400, FontStyle.NORMAL);
            font(builder, "Bold", 700, FontStyle.NORMAL);
            font(builder, "Italic", 400, FontStyle.ITALIC);
            font(builder, "BoldItalic", 700, FontStyle.ITALIC);
            builder.withHtmlContent(html(cv, avatarDataUri(cv.profile())), null);
            builder.toStream(out);
            builder.run();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return out.toByteArray();
    }

    private static void font(PdfRendererBuilder builder, String face, int weight, FontStyle style) {
        builder.useFont(() -> CvPdfRenderer.class.getResourceAsStream(FONT_DIR + face + ".ttf"), "CvSans", weight, style, true);
    }

    static String heading(CvSection.Type type) {
        return switch (type) {
            case SUMMARY -> "Summary";
            case SKILLS -> "Skills";
            case LANGUAGES -> "Foreign languages";
            case INTERNSHIP -> "Internship";
            case PROJECTS -> "Projects";
            case EDUCATION -> "Education";
            case CERTIFICATES -> "Certificates";
        };
    }

    String html(CvService.Rendered cv, String avatar) {
        CandidateProfile p = cv.profile();
        var h = new StringBuilder(12_288);
        h.append("<!DOCTYPE html><html lang=\"vi\"><head><meta charset=\"UTF-8\"/><title>").append(esc(p.getFullName()))
            .append("</title><style>").append(css()).append("</style></head><body>");
        // Nền cột phải lặp lại trên mọi trang; hàng thead/tfoot rỗng lặp lại để trang sau cũng có lề trên/dưới.
        h.append("<div class=\"side-bg\"></div><table class=\"layout\"><thead><tr><td class=\"gap\"></td><td class=\"gap\"></td></tr></thead>")
            .append("<tfoot><tr><td class=\"gap\"></td><td class=\"gap\"></td></tr></tfoot><tbody><tr><td class=\"main\">");

        // Dải đỏ đầu trang.
        h.append("<div class=\"band\"><div class=\"brand\">RIKKEI JOBS · CV</div><div class=\"name\">").append(esc(p.getFullName())).append("</div>");
        String position = notBlank(cv.cv().getTargetPosition()) ? cv.cv().getTargetPosition() : p.getHeadline();
        if (notBlank(position)) h.append("<div class=\"position\">").append(esc(position)).append("</div>");
        cv.sections().stream().filter(s -> s.section() == CvSection.Type.SUMMARY).findFirst()
            .ifPresent(s -> h.append("<div class=\"summary\">").append(multiline(s.text())).append("</div>"));
        h.append("</div>");

        for (SectionView s : cv.sections()) {
            if (s.section() == CvSection.Type.SUMMARY || SIDE.contains(s.section())) continue;
            h.append("<div class=\"section\"><div class=\"title\">").append(heading(s.section())).append("</div>");
            if (s.section() == CvSection.Type.SKILLS) skills(h, s.items());
            else for (ItemView i : s.items()) experience(h, i);
            h.append("</div>");
        }
        h.append("<div class=\"footer-note\">Generated from the Rikkei Jobs profile on ").append(clock.instant().atZone(ZONE).format(DAY))
            .append(". Verified data comes from Rikkei learning, exam and internship records; self-assessed scores are rated by the candidate.</div>");

        // Cột phụ bên phải.
        h.append("</td><td class=\"side\">");
        if (avatar != null) h.append("<div class=\"avatar\"><img src=\"").append(avatar).append("\" width=\"112\" height=\"112\" alt=\"\"/></div>");
        else h.append("<div class=\"avatar\"><div class=\"initials\">").append(esc(initials(p.getFullName()))).append("</div></div>");
        h.append("<table class=\"contact\">");
        contact(h, "Phone", p.getPhone());
        contact(h, "Email", p.getEmail());
        contact(h, "Birth", p.getDob() == null ? null : p.getDob().format(DAY));
        contact(h, "Address", p.getAddress());
        var study = new ArrayList<String>();
        if (notBlank(p.getProgram())) study.add(p.getProgram());
        if (notBlank(p.getCourseName())) study.add(p.getCourseName());
        if (notBlank(p.getClassCode())) study.add(p.getClassCode());
        contact(h, "Rikkei", study.isEmpty() ? null : String.join(" · ", study));
        h.append("</table>");
        // QR ngay dưới liên hệ để luôn nằm ở trang 1.
        h.append("<table class=\"qr\"><tr><td class=\"qr-img\"><img src=\"").append(qrDataUri(cv.publicUrl()))
            .append("\" width=\"60\" height=\"60\" alt=\"QR\"/></td><td class=\"qr-note\">Scan to view the profile verified by Rikkei</td></tr></table>");
        for (SectionView s : cv.sections()) if (SIDE.contains(s.section())) side(h, s);
        h.append("</td></tr></tbody></table></body></html>");
        return h.toString();
    }

    private static void contact(StringBuilder h, String label, String value) {
        if (!notBlank(value)) return;
        h.append("<tr><td class=\"contact-label\">").append(label).append("</td><td class=\"contact-value\">")
            .append(esc(value)).append("</td></tr>");
    }

    /** Kỹ năng gom theo nhóm, mỗi kỹ năng là một chip ngắn để cả phần chỉ chiếm vài dòng. */
    private static void skills(StringBuilder h, List<ItemView> items) {
        Map<String, List<ItemView>> byGroup = new LinkedHashMap<>();
        for (ItemView i : items) byGroup.computeIfAbsent(notBlank(i.subtitle()) ? i.subtitle() : "Other", k -> new ArrayList<>()).add(i);
        h.append("<table class=\"skills\">");
        for (var group : byGroup.entrySet()) {
            h.append("<tr><td class=\"skill-group\">").append(esc(group.getKey())).append("</td><td>");
            for (ItemView i : group.getValue()) {
                boolean verified = Boolean.TRUE.equals(i.verified());
                var m = SKILL_SCORE.matcher(i.detail() == null ? "" : i.detail());
                h.append("<span class=\"chip").append(verified ? " ok" : "").append("\">").append(esc(i.title()));
                if (m.find()) h.append(" <span class=\"chip-score\">").append(m.group(1)).append("</span>");
                h.append("</span> ");
            }
            h.append("</td></tr>");
        }
        h.append("</table><div class=\"legend\"><span class=\"dot ok\"></span> Verified by Rikkei <span class=\"dot\"></span> Self-assessed · score /100</div>");
    }

    private static void experience(StringBuilder h, ItemView i) {
        h.append("<div class=\"entry\">");
        if (notBlank(i.period())) h.append("<div class=\"entry-period\"><span class=\"bullet\"></span> ").append(esc(english(i.period()))).append("</div>");
        h.append("<div class=\"entry-title\">");
        if (notBlank(i.subtitle())) h.append(esc(i.subtitle())).append(" <span class=\"at\">at</span> ");
        h.append(esc(i.title()));
        if (Boolean.TRUE.equals(i.verified())) h.append(" <span class=\"verified\">VERIFIED</span>");
        h.append("</div>");
        var bullets = new ArrayList<String>();
        String tech = null;
        if (notBlank(i.detail())) {
            for (String line : i.detail().split("\n")) {
                if (line.startsWith(TECH_PREFIX)) tech = line.substring(TECH_PREFIX.length());
                else if (notBlank(line)) bullets.add(line.strip());
            }
        }
        if (!bullets.isEmpty() || tech != null) {
            h.append("<ul>");
            for (String b : bullets) h.append("<li>").append(esc(b)).append("</li>");
            if (tech != null) h.append("<li><b>Tech stack:</b> ").append(esc(tech)).append("</li>");
            h.append("</ul>");
        }
        h.append("</div>");
    }

    private static void side(StringBuilder h, SectionView s) {
        h.append("<div class=\"side-section\"><div class=\"title\">").append(heading(s.section())).append("</div>");
        for (ItemView i : s.items()) {
            h.append("<div class=\"side-item\"><div class=\"side-item-title\">").append(esc(i.title())).append("</div>");
            if (notBlank(i.subtitle())) h.append("<div class=\"side-sub\">").append(esc(i.subtitle())).append("</div>");
            if (notBlank(i.period())) h.append("<div class=\"side-period\">").append(esc(english(i.period()))).append("</div>");
            if (notBlank(i.detail())) h.append("<div class=\"side-muted\">").append(multiline(i.detail())).append("</div>");
            h.append("</div>");
        }
        h.append("</div>");
    }

    /** Khoảng thời gian do CvService dựng bằng tiếng Việt; CV tiếng Anh đổi các từ cố định. */
    private static String english(String period) {
        return period.replace(" – nay", " – Present").replace("Cấp ", "Issued ").replace(" · Hết hạn ", " · Expires ");
    }

    private static String css() {
        return """
            @page { size: A4; margin: 0; }
            body { margin: 0; font-family: 'CvSans'; font-size: 9.5pt; color: #1F2937; line-height: 1.45; }
            .side-bg { position: fixed; right: 0; top: 0; width: 70mm; height: 297mm; background: %2$s; z-index: -1; }
            table { border-collapse: collapse; }
            .layout { width: 100%%; -fs-table-paginate: paginate; }
            .gap { height: 9mm; padding: 0; }
            .main { padding: 0 8mm 0 0; vertical-align: top; }
            .side { width: 56mm; padding: 0 7mm; vertical-align: top; }
            .band { background: %1$s; color: #FFFFFF; padding: 6mm 8mm 6mm 10mm; margin-bottom: 5mm; border-radius: 0 3mm 3mm 0; }
            .brand { font-size: 7.5pt; letter-spacing: 2px; color: #FECACA; font-weight: 700; }
            .name { font-size: 20pt; font-weight: 700; line-height: 1.2; margin-top: 1.5mm; }
            .position { font-size: 9pt; letter-spacing: 2.5px; text-transform: uppercase; color: #FEE2E2; margin-top: 1mm; }
            .summary { font-size: 9pt; color: #FFF5F5; margin-top: 3mm; text-align: justify; }
            .section { margin: 0 0 4.5mm 10mm; }
            .title { color: %1$s; font-size: 11pt; font-weight: 700; letter-spacing: 1.5px; text-transform: uppercase;
              border-bottom: 1pt solid #FECACA; padding-bottom: 1mm; margin-bottom: 2.5mm; }
            .skills { width: 100%%; }
            .skills td { padding: 0.8mm 0; vertical-align: top; }
            .skill-group { width: 24mm; font-weight: 700; color: %1$s; font-size: 9pt; }
            .chip { display: inline-block; border: 0.8pt solid #E5E7EB; color: #4B5563; background: #FFFFFF;
              padding: 0.2mm 2mm; border-radius: 3mm; font-size: 8.5pt; margin: 0 0.8mm 1.2mm 0; }
            .chip.ok { border-color: #FCA5A5; background: #FFF1F2; color: #1F2937; }
            .chip-score { color: %1$s; font-weight: 700; }
            .legend { font-size: 7.5pt; color: #6B7280; margin-top: 0.5mm; }
            .dot { display: inline-block; width: 2.4mm; height: 2.4mm; border-radius: 1.2mm; border: 0.8pt solid #D1D5DB; background: #FFFFFF; }
            .dot.ok { border-color: #FCA5A5; background: #FFF1F2; }
            .entry { margin-bottom: 3.5mm; border-left: 1pt solid #FECACA; padding-left: 4mm; page-break-inside: avoid; }
            .entry-period { font-weight: 700; font-size: 8.5pt; color: %1$s; margin-left: -5.6mm; }
            .bullet { display: inline-block; width: 2.4mm; height: 2.4mm; border-radius: 1.2mm; background: %1$s; }
            .entry-title { font-weight: 700; font-size: 10pt; color: #111827; margin-top: 0.5mm; }
            .at { font-weight: 400; color: #6B7280; }
            .verified { font-size: 6.5pt; font-weight: 700; letter-spacing: 1px; color: %1$s; border: 0.8pt solid %1$s;
              padding: 0 1.5mm; border-radius: 2mm; }
            ul { margin: 1mm 0 0 0; padding-left: 4mm; color: #374151; }
            li { margin-bottom: 0.5mm; }
            .avatar { text-align: center; margin: 0 0 4mm 0; }
            .initials { width: 30mm; height: 30mm; line-height: 30mm; margin: 0 auto; border-radius: 15mm; background: %1$s;
              color: #FFFFFF; font-size: 24pt; font-weight: 700; text-align: center; }
            .contact { width: 100%%; }
            .contact td { padding: 0.9mm 0; vertical-align: top; font-size: 8.5pt; }
            .contact-label { width: 18mm; color: %1$s; font-weight: 700; font-size: 7pt; text-transform: uppercase; letter-spacing: 0.5px; padding-top: 1.2mm; }
            .contact-value { word-wrap: break-word; color: #374151; }
            .qr { width: 100%%; margin: 3mm 0 1mm 0; background: #FFFFFF; border: 0.8pt solid #FECACA; border-radius: 2.5mm; }
            .qr td { padding: 1.8mm; vertical-align: middle; }
            .qr-img { width: 60px; }
            .qr-note { color: #6B7280; font-size: 7.5pt; line-height: 1.3; }
            .side-section { margin-top: 5mm; }
            .side-item { margin-bottom: 2.5mm; font-size: 8.8pt; page-break-inside: avoid; }
            .side-item-title { font-weight: 700; color: %1$s; font-size: 9.5pt; line-height: 1.3; }
            .side-sub { font-weight: 700; color: #374151; }
            .side-period { color: #374151; }
            .side-muted { color: #6B7280; }
            .footer-note { margin: 2mm 0 0 10mm; font-size: 7pt; color: #9CA3AF; }
            """.formatted(BRAND, SOFT);
    }

    /** Ảnh đại diện cắt tròn sẵn bằng Java (trình dựng PDF không cắt ảnh theo border-radius). Lỗi thì dùng chữ viết tắt. */
    private String avatarDataUri(CandidateProfile p) {
        if (!notBlank(p.getAvatarUrl())) return null;
        try {
            var file = files.download(p.getUserId(), files.requireOwned(p.getUserId(), p.getAvatarUrl()).getId());
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(file.bytes()));
            if (src == null) return null;
            int size = 360, ring = 12;
            int side = Math.min(src.getWidth(), src.getHeight());
            var square = src.getSubimage((src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side);
            var out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fill(new Ellipse2D.Double(0, 0, size, size));
            g.setClip(new Ellipse2D.Double(ring, ring, size - 2.0 * ring, size - 2.0 * ring));
            g.drawImage(square, ring, ring, size - 2 * ring, size - 2 * ring, null);
            g.dispose();
            return png(out);
        } catch (RuntimeException | IOException ex) {
            log.warn("Không dựng được ảnh đại diện cho CV của {}: {}", p.getUserId(), ex.getMessage());
            return null;
        }
    }

    static String qrDataUri(String url) {
        try {
            BitMatrix m = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, Map.of(EncodeHintType.MARGIN, 1));
            int scale = 6;
            var img = new BufferedImage(m.getWidth() * scale, m.getHeight() * scale, BufferedImage.TYPE_BYTE_BINARY);
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) img.setRGB(x, y, m.get(x / scale, y / scale) ? 0x000000 : 0xFFFFFF);
            }
            return png(img);
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Không tạo được mã QR", ex);
        }
    }

    private static String png(BufferedImage img) throws IOException {
        var png = new ByteArrayOutputStream();
        ImageIO.write(img, "png", png);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray());
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.strip().split("\\s+");
        String last = words[words.length - 1];
        return (words.length > 1 ? words[words.length - 2].substring(0, 1) : "") + last.substring(0, 1);
    }

    private static String multiline(String s) { return esc(s).replace("\n", "<br/>"); }

    /** Escape ký tự đặc biệt XML và bỏ ký tự điều khiển (trình phân tích XHTML từ chối chúng). */
    static String esc(String s) {
        if (s == null) return "";
        return HtmlUtils.htmlEscape(s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""), "UTF-8");
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
