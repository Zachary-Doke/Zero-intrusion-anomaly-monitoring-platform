const fs = require("fs");
const path = require("path");
const {
  AlignmentType,
  BorderStyle,
  Document,
  Footer,
  HeadingLevel,
  LevelFormat,
  Packer,
  PageBreak,
  PageNumber,
  Paragraph,
  ShadingType,
  TextRun,
  Table,
  TableCell,
  TableRow,
  VerticalAlign,
  WidthType,
} = require("/home/doke/.codex/skills/docx/node_modules/docx");

const inputPath = process.argv[2];
const outputPath = process.argv[3];

if (!inputPath || !outputPath) {
  console.error("Usage: node scripts/generate_thesis_docx.js <input.md> <output.docx>");
  process.exit(1);
}

const md = fs.readFileSync(inputPath, "utf8").replace(/\r\n/g, "\n");
const lines = md.split("\n");

const bodyFont = "宋体";
const latinFont = "Times New Roman";
const codeFont = "Consolas";
const usableWidth = 9028;
const tableBorder = { style: BorderStyle.SINGLE, size: 1, color: "BFBFBF" };
const cellBorders = {
  top: tableBorder,
  bottom: tableBorder,
  left: tableBorder,
  right: tableBorder,
};

function cleanText(text) {
  return text
    .replace(/\r/g, "")
    .replace(/\[(.*?)\]\((.*?)\)/g, "$1")
    .replace(/\*\*(.*?)\*\*/g, "$1")
    .replace(/\*(.*?)\*/g, "$1");
}

function inlineRuns(text, options = {}) {
  const runs = [];
  const parts = text.split(/(`[^`]+`)/g).filter(Boolean);
  for (const part of parts) {
    if (part.startsWith("`") && part.endsWith("`")) {
      runs.push(
        new TextRun({
          text: part.slice(1, -1),
          font: codeFont,
          size: options.size || 24,
        })
      );
      continue;
    }
    if (!part) {
      continue;
    }
    runs.push(
      new TextRun({
        text: cleanText(part),
        bold: options.bold || false,
        italics: options.italics || false,
        font: options.font || bodyFont,
        size: options.size || 24,
      })
    );
  }
  return runs.length ? runs : [new TextRun({ text: "", font: bodyFont, size: 24 })];
}

function bodyParagraph(text, extra = {}) {
  const cleaned = cleanText(text);
  return new Paragraph({
    alignment: extra.alignment || AlignmentType.JUSTIFIED,
    spacing: extra.spacing || { after: 160, line: 420 },
    indent: extra.indent || { firstLine: 480 },
    pageBreakBefore: extra.pageBreakBefore || false,
    children: inlineRuns(cleaned, extra.run || {}),
  });
}

function headingParagraph(text, level, pageBreakBefore = false) {
  const mapping = {
    2: HeadingLevel.HEADING_1,
    3: HeadingLevel.HEADING_2,
    4: HeadingLevel.HEADING_3,
  };
  return new Paragraph({
    heading: mapping[level] || HeadingLevel.HEADING_4,
    pageBreakBefore,
    spacing: { before: 240, after: 180 },
    children: [new TextRun({ text: cleanText(text), font: bodyFont })],
  });
}

function codeParagraph(text) {
  return new Paragraph({
    spacing: { after: 0, line: 360 },
    indent: { left: 360 },
    children: [
      new TextRun({
        text,
        font: codeFont,
        size: 20,
      }),
    ],
  });
}

function parseTableRows(tableLines) {
  const rows = tableLines.map((line) =>
    line
      .trim()
      .replace(/^\|/, "")
      .replace(/\|$/, "")
      .split("|")
      .map((cell) => cleanText(cell.trim()))
  );
  if (rows.length > 1 && rows[1].every((cell) => /^:?-{3,}:?$/.test(cell))) {
    rows.splice(1, 1);
  }
  return rows;
}

function buildTable(tableLines) {
  const rows = parseTableRows(tableLines);
  const colCount = rows[0]?.length || 1;
  const colWidth = Math.floor(usableWidth / colCount);
  return new Table({
    columnWidths: Array(colCount).fill(colWidth),
    margins: { top: 80, bottom: 80, left: 120, right: 120 },
    rows: rows.map((row, rowIndex) =>
      new TableRow({
        tableHeader: rowIndex === 0,
        children: row.map(
          (cell) =>
            new TableCell({
              width: { size: colWidth, type: WidthType.DXA },
              borders: cellBorders,
              verticalAlign: VerticalAlign.CENTER,
              shading:
                rowIndex === 0
                  ? { fill: "EAF2F8", type: ShadingType.CLEAR }
                  : undefined,
              children: [
                new Paragraph({
                  alignment: rowIndex === 0 ? AlignmentType.CENTER : AlignmentType.LEFT,
                  spacing: { after: 80, line: 360 },
                  children: inlineRuns(cell, {
                    bold: rowIndex === 0,
                    size: rowIndex === 0 ? 22 : 22,
                  }),
                }),
              ],
            })
        ),
      })
    ),
  });
}

function buildList(items, ordered, reference) {
  return items.map((item) => {
    const text = item.replace(/^(\d+)\.\s+/, "").replace(/^-\s+/, "");
    return new Paragraph({
      numbering: { reference, level: 0 },
      spacing: { after: 120, line: 420 },
      children: inlineRuns(text),
    });
  });
}

function extractCover(linesArr) {
  const cover = [];
  let index = 0;
  while (index < linesArr.length && cover.length < 3) {
    const line = linesArr[index].trim();
    if (line.startsWith("#")) {
      cover.push(line);
    }
    index += 1;
  }
  return { cover, startIndex: index };
}

const { cover, startIndex } = extractCover(lines);

const blocks = [];
let paragraphBuffer = [];
let listBuffer = [];
let listType = null;
let tableBuffer = [];
let inCode = false;
let codeBuffer = [];

function flushParagraph() {
  if (!paragraphBuffer.length) {
    return;
  }
  blocks.push({ type: "paragraph", text: paragraphBuffer.join(" ").trim() });
  paragraphBuffer = [];
}

function flushList() {
  if (!listBuffer.length) {
    return;
  }
  blocks.push({ type: "list", ordered: listType === "ordered", items: [...listBuffer] });
  listBuffer = [];
  listType = null;
}

function flushTable() {
  if (!tableBuffer.length) {
    return;
  }
  blocks.push({ type: "table", lines: [...tableBuffer] });
  tableBuffer = [];
}

function flushCode() {
  if (!codeBuffer.length) {
    return;
  }
  blocks.push({ type: "code", lines: [...codeBuffer] });
  codeBuffer = [];
}

for (let i = startIndex; i < lines.length; i += 1) {
  const raw = lines[i];
  const line = raw.trimEnd();
  const trimmed = line.trim();

  if (trimmed.startsWith("```")) {
    flushParagraph();
    flushList();
    flushTable();
    if (inCode) {
      flushCode();
      inCode = false;
    } else {
      inCode = true;
    }
    continue;
  }

  if (inCode) {
    codeBuffer.push(line);
    continue;
  }

  if (!trimmed) {
    flushParagraph();
    flushList();
    flushTable();
    continue;
  }

  if (/^#{2,4}\s+/.test(trimmed)) {
    flushParagraph();
    flushList();
    flushTable();
    const level = trimmed.match(/^#+/)[0].length;
    blocks.push({ type: "heading", level, text: trimmed.replace(/^#{2,4}\s+/, "") });
    continue;
  }

  if (trimmed.startsWith("|")) {
    flushParagraph();
    flushList();
    tableBuffer.push(trimmed);
    continue;
  }

  if (/^\d+\.\s+/.test(trimmed)) {
    flushParagraph();
    flushTable();
    if (listType && listType !== "ordered") {
      flushList();
    }
    listType = "ordered";
    listBuffer.push(trimmed);
    continue;
  }

  if (/^-\s+/.test(trimmed)) {
    flushParagraph();
    flushTable();
    if (listType && listType !== "bullet") {
      flushList();
    }
    listType = "bullet";
    listBuffer.push(trimmed);
    continue;
  }

  flushList();
  flushTable();
  paragraphBuffer.push(trimmed.replace(/^>\s*/, ""));
}

flushParagraph();
flushList();
flushTable();
flushCode();

const numbering = { config: [] };
let listRefSeq = 0;
const children = [];

for (const block of blocks) {
  if (block.type === "heading") {
    const topLevel = block.level === 2;
    children.push(headingParagraph(block.text, block.level, topLevel));
    continue;
  }

  if (block.type === "paragraph") {
    const centered = /^图\s+\d+-\d+/.test(block.text) || /^图\s+\d+/.test(block.text);
    children.push(
      bodyParagraph(block.text, {
        alignment: centered ? AlignmentType.CENTER : AlignmentType.JUSTIFIED,
        indent: centered ? undefined : { firstLine: 480 },
        spacing: centered ? { after: 160, line: 360 } : { after: 160, line: 420 },
      })
    );
    continue;
  }

  if (block.type === "list") {
    listRefSeq += 1;
    const reference = `${block.ordered ? "numbered" : "bullet"}-${listRefSeq}`;
    numbering.config.push({
      reference,
      levels: [
        {
          level: 0,
          format: block.ordered ? LevelFormat.DECIMAL : LevelFormat.BULLET,
          text: block.ordered ? "%1." : "•",
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } },
        },
      ],
    });
    children.push(...buildList(block.items, block.ordered, reference));
    continue;
  }

  if (block.type === "table") {
    children.push(buildTable(block.lines));
    children.push(new Paragraph({ spacing: { after: 160 } }));
    continue;
  }

  if (block.type === "code") {
    children.push(
      new Paragraph({
        spacing: { after: 60 },
        children: [new TextRun({ text: "代码块", bold: true, font: bodyFont, size: 22 })],
      })
    );
    for (const codeLine of block.lines) {
      children.push(codeParagraph(codeLine));
    }
    children.push(new Paragraph({ spacing: { after: 160 } }));
  }
}

const coverTitle = (cover[0] || "# 本科毕业设计（论文）").replace(/^#\s*/, "").trim();
const subject = (cover[1] || "## 题目：零侵入异常监控平台设计与实现")
  .replace(/^##\s*/, "")
  .trim();
const englishTitle = (cover[2] || "### Design and Implementation of a Zero-Intrusion Exception Monitoring Platform")
  .replace(/^###\s*/, "")
  .trim();

const coverChildren = [
  new Paragraph({ spacing: { after: 1200 } }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 600 },
    children: [new TextRun({ text: coverTitle, bold: true, font: bodyFont, size: 36 })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 360 },
    children: [new TextRun({ text: subject, bold: true, font: bodyFont, size: 32 })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 360 },
    children: [new TextRun({ text: englishTitle, italics: true, font: latinFont, size: 28 })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [new TextRun({ text: "作者：Zachary-Doke", font: bodyFont, size: 24 })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [new TextRun({ text: "专业：软件工程", font: bodyFont, size: 24 })],
  }),
  new Paragraph({
    alignment: AlignmentType.CENTER,
    spacing: { after: 240 },
    children: [new TextRun({ text: "日期：2026年4月8日", font: bodyFont, size: 24 })],
  }),
  new Paragraph({ children: [new PageBreak()] }),
];

const doc = new Document({
  numbering,
  styles: {
    default: {
      document: {
        run: {
          font: bodyFont,
          size: 24,
        },
      },
    },
    paragraphStyles: [
      {
        id: "Heading1",
        name: "Heading 1",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { font: bodyFont, size: 32, bold: true, color: "000000" },
        paragraph: { spacing: { before: 240, after: 180 }, outlineLevel: 0 },
      },
      {
        id: "Heading2",
        name: "Heading 2",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { font: bodyFont, size: 28, bold: true, color: "000000" },
        paragraph: { spacing: { before: 220, after: 140 }, outlineLevel: 1 },
      },
      {
        id: "Heading3",
        name: "Heading 3",
        basedOn: "Normal",
        next: "Normal",
        quickFormat: true,
        run: { font: bodyFont, size: 26, bold: true, color: "000000" },
        paragraph: { spacing: { before: 180, after: 120 }, outlineLevel: 2 },
      },
    ],
  },
  sections: [
    {
      properties: {
        page: {
          margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 },
          size: { width: 11906, height: 16838 },
        },
      },
      footers: {
        default: new Footer({
          children: [
            new Paragraph({
              alignment: AlignmentType.CENTER,
              children: [new TextRun({ text: "", font: bodyFont, size: 20 }), new TextRun({ children: [PageNumber.CURRENT] })],
            }),
          ],
        }),
      },
      children: [...coverChildren, ...children],
    },
  ],
});

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(outputPath, buffer);
  console.log(`Wrote ${path.resolve(outputPath)}`);
});
