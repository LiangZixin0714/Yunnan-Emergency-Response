package com.project.service;

import com.project.entity.mysql.Plan;
import com.project.repository.mysql.PlanRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.BulletList;
import org.commonmark.node.OrderedList;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.DocumentException;
import java.awt.Color;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PlanRepository planRepository;
    private final Parser markdownParser;

    public ExportService(PlanRepository planRepository) {
        this.planRepository = planRepository;
        this.markdownParser = Parser.builder().build();
    }

    public byte[] exportToPdf(String planId) throws Exception {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在，planId: " + planId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        Document pdfDoc = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(pdfDoc, baos);
        pdfDoc.open();

        BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        Font titleFont = new Font(bfChinese, 18, Font.BOLD);
        Font headerFont = new Font(bfChinese, 14, Font.BOLD);
        Font normalFont = new Font(bfChinese, 11, Font.NORMAL);
        Font metaFont = new Font(bfChinese, 9, Font.NORMAL, Color.GRAY);

        Paragraph title = new Paragraph(plan.getPlanTitle(), titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        pdfDoc.add(title);

        Paragraph meta = new Paragraph();
        meta.setAlignment(Element.ALIGN_RIGHT);
        meta.setSpacingAfter(15);
        
        StringBuilder metaText = new StringBuilder();
        if (plan.getGenerateTime() != null) {
            metaText.append("生成时间：").append(plan.getGenerateTime().format(DATE_FORMATTER));
        }
        if (plan.getStatus() != null) {
            metaText.append("  |  状态：").append(getStatusLabel(plan.getStatus()));
        }
        meta.add(new Chunk(metaText.toString(), metaFont));
        pdfDoc.add(meta);

        pdfDoc.add(new Paragraph("\n"));

        String content = plan.getPlanContent();
        if (content != null && !content.isEmpty()) {
            Node documentNode = markdownParser.parse(content);
            renderMarkdownToPdf(documentNode, pdfDoc, headerFont, normalFont);
        }

        pdfDoc.close();
        return baos.toByteArray();
    }

    private void renderMarkdownToPdf(Node node, Document pdfDoc, Font headerFont, Font normalFont) throws DocumentException {
        if (node instanceof org.commonmark.node.Document) {
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                renderMarkdownToPdf(child, pdfDoc, headerFont, normalFont);
            }
        } else if (node instanceof Heading) {
            Heading heading = (Heading) node;
            int level = heading.getLevel();
            Font font = switch (level) {
                case 1 -> new Font(headerFont.getBaseFont(), 16, Font.BOLD);
                case 2 -> new Font(headerFont.getBaseFont(), 14, Font.BOLD);
                case 3 -> new Font(headerFont.getBaseFont(), 12, Font.BOLD);
                default -> new Font(headerFont.getBaseFont(), 11, Font.BOLD);
            };
            Paragraph p = new Paragraph(getTextContent(heading), font);
            p.setSpacingBefore(12);
            p.setSpacingAfter(6);
            pdfDoc.add(p);
        } else if (node instanceof org.commonmark.node.Paragraph) {
            Paragraph p = new Paragraph(getTextContent((org.commonmark.node.Paragraph) node), normalFont);
            p.setSpacingAfter(8);
            p.setFirstLineIndent(22);
            pdfDoc.add(p);
        } else if (node instanceof BulletList) {
            BulletList list = (BulletList) node;
            for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof org.commonmark.node.ListItem) {
                    Paragraph p = new Paragraph("• " + getTextContent((org.commonmark.node.ListItem) child), normalFont);
                    p.setSpacingAfter(4);
                    pdfDoc.add(p);
                }
            }
        } else if (node instanceof OrderedList) {
            OrderedList list = (OrderedList) node;
            int index = 1;
            for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof org.commonmark.node.ListItem) {
                    Paragraph p = new Paragraph(index + ". " + getTextContent((org.commonmark.node.ListItem) child), normalFont);
                    p.setSpacingAfter(4);
                    pdfDoc.add(p);
                    index++;
                }
            }
        } else if (node instanceof BlockQuote) {
            Paragraph p = new Paragraph("【引用】" + getTextContent((BlockQuote) node), normalFont);
            p.setSpacingAfter(8);
            p.setIndentationLeft(20);
            p.setIndentationRight(20);
            pdfDoc.add(p);
        } else if (node instanceof ThematicBreak) {
            Paragraph p = new Paragraph("─────────────────────────────────────────────", normalFont);
            p.setSpacingBefore(10);
            p.setSpacingAfter(10);
            pdfDoc.add(p);
        }
    }

    private String getTextContent(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text) {
                sb.append(((Text) child).getLiteral());
            } else if (child instanceof SoftLineBreak || child instanceof HardLineBreak) {
                sb.append(" ");
            } else {
                sb.append(getTextContent(child));
            }
        }
        return sb.toString().trim();
    }

    public byte[] exportToWord(String planId) throws Exception {
        Plan plan = planRepository.findByPlanId(planId)
                .orElseThrow(() -> new IllegalArgumentException("方案不存在，planId: " + planId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XWPFDocument wordDoc = new XWPFDocument();

            XWPFParagraph titleParagraph = wordDoc.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(plan.getPlanTitle());
            titleRun.setFontSize(18);
            titleRun.setBold(true);
            titleRun.setFontFamily("宋体");
            titleParagraph.setSpacingAfter(200);

            XWPFParagraph metaParagraph = wordDoc.createParagraph();
            metaParagraph.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun metaRun = metaParagraph.createRun();
            StringBuilder metaText = new StringBuilder();
            if (plan.getGenerateTime() != null) {
                metaText.append("生成时间：").append(plan.getGenerateTime().format(DATE_FORMATTER));
            }
            if (plan.getStatus() != null) {
                metaText.append("  |  状态：").append(getStatusLabel(plan.getStatus()));
            }
            metaRun.setText(metaText.toString());
            metaRun.setFontSize(9);
            metaRun.setColor("808080");
            metaRun.setFontFamily("宋体");
            metaParagraph.setSpacingAfter(150);

            String content = plan.getPlanContent();
            if (content != null && !content.isEmpty()) {
                Node documentNode = markdownParser.parse(content);
                renderMarkdownToWord(documentNode, wordDoc);
            }

            wordDoc.write(baos);
            return baos.toByteArray();
        }
    }

    private void renderMarkdownToWord(Node node, XWPFDocument wordDoc) {
        if (node instanceof org.commonmark.node.Document) {
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                renderMarkdownToWord(child, wordDoc);
            }
        } else if (node instanceof Heading) {
            Heading heading = (Heading) node;
            int level = heading.getLevel();
            XWPFParagraph paragraph = wordDoc.createParagraph();
            paragraph.setSpacingBefore(120);
            paragraph.setSpacingAfter(60);
            
            XWPFRun run = paragraph.createRun();
            run.setText(getTextContent(heading));
            run.setBold(true);
            run.setFontFamily("黑体");
            run.setFontSize(switch (level) {
                case 1 -> 16;
                case 2 -> 14;
                case 3 -> 12;
                default -> 11;
            });
        } else if (node instanceof org.commonmark.node.Paragraph) {
            XWPFParagraph paragraph = wordDoc.createParagraph();
            paragraph.setSpacingAfter(80);
            paragraph.setFirstLineIndent(420);
            
            XWPFRun run = paragraph.createRun();
            run.setText(getTextContent((org.commonmark.node.Paragraph) node));
            run.setFontFamily("宋体");
            run.setFontSize(11);
        } else if (node instanceof BulletList) {
            BulletList list = (BulletList) node;
            for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof org.commonmark.node.ListItem) {
                    XWPFParagraph paragraph = wordDoc.createParagraph();
                    paragraph.setSpacingAfter(40);
                    paragraph.setIndentationFirstLine(-210);
                    paragraph.setIndentationLeft(210);
                    
                    XWPFRun run = paragraph.createRun();
                    run.setText("• " + getTextContent((org.commonmark.node.ListItem) child));
                    run.setFontFamily("宋体");
                    run.setFontSize(11);
                }
            }
        } else if (node instanceof OrderedList) {
            OrderedList list = (OrderedList) node;
            int index = 1;
            for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof org.commonmark.node.ListItem) {
                    XWPFParagraph paragraph = wordDoc.createParagraph();
                    paragraph.setSpacingAfter(40);
                    paragraph.setIndentationFirstLine(-210);
                    paragraph.setIndentationLeft(210);
                    
                    XWPFRun run = paragraph.createRun();
                    run.setText(index + ". " + getTextContent((org.commonmark.node.ListItem) child));
                    run.setFontFamily("宋体");
                    run.setFontSize(11);
                    index++;
                }
            }
        } else if (node instanceof BlockQuote) {
            XWPFParagraph paragraph = wordDoc.createParagraph();
            paragraph.setSpacingAfter(80);
            paragraph.setIndentationLeft(200);
            paragraph.setIndentationRight(200);
            
            XWPFRun run = paragraph.createRun();
            run.setText("【引用】" + getTextContent((BlockQuote) node));
            run.setFontFamily("宋体");
            run.setFontSize(11);
            run.setItalic(true);
        } else if (node instanceof ThematicBreak) {
            XWPFParagraph paragraph = wordDoc.createParagraph();
            paragraph.setSpacingBefore(100);
            paragraph.setSpacingAfter(100);
            
            XWPFRun run = paragraph.createRun();
            run.setText("─────────────────────────────────────────────");
            run.setFontFamily("宋体");
            run.setFontSize(11);
        }
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "draft" -> "草稿";
            case "submitted" -> "已提交";
            case "accepted" -> "已接受";
            case "rejected" -> "已驳回";
            default -> status;
        };
    }
}