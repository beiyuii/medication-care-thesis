from __future__ import annotations

import math
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable

from PIL import Image, ImageDraw, ImageFont


ROOT = Path("/Users/beiyuii/Desktop/李怡蕾/毕设")
OUTPUT_DIR = ROOT / "output" / "er-diagrams"

SVG_BG = "#ffffff"
STROKE = "#111111"
FILL = "#ffffff"
TEXT = "#111111"


@dataclass
class Node:
    id: str
    kind: str
    x: float
    y: float
    w: float
    h: float
    label: str
    font_size: int = 28


@dataclass
class Edge:
    start: str
    end: str
    label_start: str | None = None
    label_end: str | None = None


@dataclass
class Diagram:
    name: str
    title: str
    width: int
    height: int
    nodes: list[Node] = field(default_factory=list)
    edges: list[Edge] = field(default_factory=list)


ENTITY_SPECS = {
    "users": {
        "cn": "用户账号表",
        "attrs": ["id (PK)", "username", "pwd_hash", "role"],
    },
    "patients": {
        "cn": "患者表",
        "attrs": ["id (PK)", "elder_user_id (FK)", "name"],
    },
    "user_patient_relation": {
        "cn": "用户与患者关联表",
        "attrs": ["id (PK)", "user_id (FK)", "patient_id (FK)", "relation_type"],
    },
    "schedules": {
        "cn": "用药计划表",
        "attrs": [
            "id (PK)",
            "patient_id (FK)",
            "type",
            "dose",
            "freq",
            "win_start",
            "win_end",
            "period",
            "status",
        ],
    },
    "intake_events": {
        "cn": "服药事件表",
        "attrs": [
            "id (PK)",
            "patient_id (FK)",
            "schedule_id (FK)",
            "ts",
            "status",
            "action",
            "targets_json",
            "img_url",
        ],
    },
    "alerts": {
        "cn": "告警表",
        "attrs": ["id (PK)", "patient_id (FK)", "type", "title", "ts", "status"],
    },
    "log_images": {
        "cn": "图片日志表",
        "attrs": ["id (PK)", "event_id (FK)", "url", "ts"],
    },
    "settings": {
        "cn": "用户设置表",
        "attrs": ["id (PK)", "user_id (FK)", "privacy", "notify_config"],
    },
}


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "/System/Library/Fonts/PingFang.ttc",
        "/System/Library/Fonts/Hiragino Sans GB.ttc",
        "/System/Library/Fonts/STHeiti Medium.ttc",
        "/System/Library/Fonts/Supplemental/Arial Unicode.ttf",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


def escape_svg(text: str) -> str:
    return (
        text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def wrap_label(label: str) -> list[str]:
    return label.split("\n")


def text_bbox(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont) -> tuple[int, int]:
    box = draw.textbbox((0, 0), text, font=font)
    return box[2] - box[0], box[3] - box[1]


def add_centered_multiline_text(
    draw: ImageDraw.ImageDraw,
    cx: float,
    cy: float,
    text: str,
    font: ImageFont.ImageFont,
) -> None:
    lines = wrap_label(text)
    line_boxes = [draw.textbbox((0, 0), line, font=font) for line in lines]
    line_heights = [box[3] - box[1] for box in line_boxes]
    total_height = sum(line_heights) + max(0, len(lines) - 1) * 8
    y = cy - total_height / 2
    for line, box, height in zip(lines, line_boxes, line_heights):
        width = box[2] - box[0]
        draw.text((cx - width / 2, y), line, fill=TEXT, font=font)
        y += height + 8


def render_png(diagram: Diagram, output_path: Path) -> None:
    image = Image.new("RGB", (diagram.width, diagram.height), SVG_BG)
    draw = ImageDraw.Draw(image)

    node_font_cache: dict[int, ImageFont.ImageFont] = {}

    def font_for(size: int) -> ImageFont.ImageFont:
        if size not in node_font_cache:
            node_font_cache[size] = load_font(size)
        return node_font_cache[size]

    centers = {node.id: (node.x, node.y) for node in diagram.nodes}

    for edge in diagram.edges:
        sx, sy = centers[edge.start]
        ex, ey = centers[edge.end]
        draw.line((sx, sy, ex, ey), fill=STROKE, width=3)
        if edge.label_start:
            mx = sx + (ex - sx) * 0.18
            my = sy + (ey - sy) * 0.18 - 20
            add_centered_multiline_text(draw, mx, my, edge.label_start, font_for(24))
        if edge.label_end:
            mx = sx + (ex - sx) * 0.82
            my = sy + (ey - sy) * 0.82 - 20
            add_centered_multiline_text(draw, mx, my, edge.label_end, font_for(24))

    for node in diagram.nodes:
        left = node.x - node.w / 2
        top = node.y - node.h / 2
        right = node.x + node.w / 2
        bottom = node.y + node.h / 2

        if node.kind == "entity":
            draw.rectangle((left, top, right, bottom), outline=STROKE, width=4, fill=FILL)
        elif node.kind == "attribute":
            draw.ellipse((left, top, right, bottom), outline=STROKE, width=3, fill=FILL)
        elif node.kind == "relationship":
            points = [
                (node.x, top),
                (right, node.y),
                (node.x, bottom),
                (left, node.y),
            ]
            draw.polygon(points, outline=STROKE, width=4, fill=FILL)
        else:
            raise ValueError(f"Unsupported node kind: {node.kind}")

        add_centered_multiline_text(draw, node.x, node.y, node.label, font_for(node.font_size))

    title_font = font_for(36)
    title_w, _ = text_bbox(draw, diagram.title, title_font)
    draw.text(((diagram.width - title_w) / 2, 30), diagram.title, fill=TEXT, font=title_font)

    image.save(output_path)


def svg_text(x: float, y: float, label: str, font_size: int) -> str:
    lines = wrap_label(label)
    if len(lines) == 1:
        return (
            f'<text x="{x}" y="{y}" text-anchor="middle" dominant-baseline="middle" '
            f'font-size="{font_size}" fill="{TEXT}" font-family="PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif">'
            f"{escape_svg(lines[0])}</text>"
        )
    tspans = []
    start_y = y - (len(lines) - 1) * (font_size * 0.8) / 2
    for idx, line in enumerate(lines):
        dy = start_y + idx * (font_size * 1.1)
        tspans.append(
            f'<tspan x="{x}" y="{dy}" text-anchor="middle">{escape_svg(line)}</tspan>'
        )
    return (
        f'<text font-size="{font_size}" fill="{TEXT}" font-family="PingFang SC, Hiragino Sans GB, Microsoft YaHei, sans-serif">'
        + "".join(tspans)
        + "</text>"
    )


def render_svg(diagram: Diagram, output_path: Path) -> None:
    parts: list[str] = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{diagram.width}" height="{diagram.height}" viewBox="0 0 {diagram.width} {diagram.height}">',
        f'<rect width="100%" height="100%" fill="{SVG_BG}"/>',
        svg_text(diagram.width / 2, 50, diagram.title, 36),
    ]

    centers = {node.id: (node.x, node.y) for node in diagram.nodes}

    for edge in diagram.edges:
        sx, sy = centers[edge.start]
        ex, ey = centers[edge.end]
        parts.append(
            f'<line x1="{sx}" y1="{sy}" x2="{ex}" y2="{ey}" stroke="{STROKE}" stroke-width="3"/>'
        )
        if edge.label_start:
            mx = sx + (ex - sx) * 0.18
            my = sy + (ey - sy) * 0.18 - 20
            parts.append(svg_text(mx, my, edge.label_start, 24))
        if edge.label_end:
            mx = sx + (ex - sx) * 0.82
            my = sy + (ey - sy) * 0.82 - 20
            parts.append(svg_text(mx, my, edge.label_end, 24))

    for node in diagram.nodes:
        left = node.x - node.w / 2
        top = node.y - node.h / 2
        if node.kind == "entity":
            parts.append(
                f'<rect x="{left}" y="{top}" width="{node.w}" height="{node.h}" fill="{FILL}" stroke="{STROKE}" stroke-width="4"/>'
            )
        elif node.kind == "attribute":
            parts.append(
                f'<ellipse cx="{node.x}" cy="{node.y}" rx="{node.w / 2}" ry="{node.h / 2}" fill="{FILL}" stroke="{STROKE}" stroke-width="3"/>'
            )
        elif node.kind == "relationship":
            right = node.x + node.w / 2
            bottom = node.y + node.h / 2
            points = f"{node.x},{top} {right},{node.y} {node.x},{bottom} {left},{node.y}"
            parts.append(
                f'<polygon points="{points}" fill="{FILL}" stroke="{STROKE}" stroke-width="4"/>'
            )
        parts.append(svg_text(node.x, node.y, node.label, node.font_size))

    parts.append("</svg>")
    output_path.write_text("\n".join(parts), encoding="utf-8")


def build_entity_diagram(entity_name: str, cn_name: str, attrs: Iterable[str]) -> Diagram:
    attrs = list(attrs)
    diagram = Diagram(
        name=f"entity_{entity_name}",
        title=f"{entity_name} 实体属性图（陈氏 ER）",
        width=1800,
        height=1400,
    )
    diagram.nodes.append(
        Node(
            id=f"{entity_name}_entity",
            kind="entity",
            x=900,
            y=700,
            w=320,
            h=140,
            label=f"{entity_name}\n{cn_name}",
            font_size=28,
        )
    )

    radius_x = 520 if len(attrs) <= 6 else 620
    radius_y = 380 if len(attrs) <= 6 else 450
    for index, attr in enumerate(attrs):
        angle = -math.pi / 2 + (2 * math.pi * index / len(attrs))
        x = 900 + radius_x * math.cos(angle)
        y = 700 + radius_y * math.sin(angle)
        width = max(220, min(360, 120 + len(attr) * 14))
        diagram.nodes.append(
            Node(
                id=f"{entity_name}_attr_{index}",
                kind="attribute",
                x=x,
                y=y,
                w=width,
                h=88,
                label=attr,
                font_size=24,
            )
        )
        diagram.edges.append(Edge(start=f"{entity_name}_entity", end=f"{entity_name}_attr_{index}"))
    return diagram


def build_global_diagram() -> Diagram:
    diagram = Diagram(
        name="global_chen_er",
        title="全局实体关系图（陈氏 ER）",
        width=5600,
        height=3400,
    )

    def add_entity(entity_id: str, x: int, y: int, attrs: list[str], cn: str) -> None:
        diagram.nodes.append(
            Node(
                id=entity_id,
                kind="entity",
                x=x,
                y=y,
                w=320,
                h=140,
                label=f"{entity_id}\n{cn}",
                font_size=28,
            )
        )

        if entity_id == "users":
            positions = [
                (x - 350, y - 150),
                (x - 360, y + 10),
                (x - 310, y + 170),
                (x + 310, y - 150),
            ]
        elif entity_id == "patients":
            positions = [
                (x - 340, y - 160),
                (x, y - 220),
                (x + 340, y - 160),
            ]
        elif entity_id == "user_patient_relation":
            positions = [
                (x - 420, y),
                (x, y - 240),
                (x + 420, y),
                (x, y + 240),
            ]
        elif entity_id == "settings":
            positions = [
                (x - 320, y - 140),
                (x + 320, y - 140),
                (x - 320, y + 140),
                (x + 320, y + 140),
            ]
        elif entity_id == "schedules":
            positions = [
                (x - 420, y - 170),
                (x, y - 250),
                (x + 420, y - 170),
                (x - 420, y),
                (x + 420, y),
                (x - 420, y + 170),
                (x, y + 250),
                (x + 420, y + 170),
                (x, y + 370),
            ]
        elif entity_id == "intake_events":
            positions = [
                (x - 450, y - 200),
                (x, y - 280),
                (x + 450, y - 200),
                (x - 450, y),
                (x + 450, y),
                (x - 450, y + 200),
                (x, y + 300),
                (x + 450, y + 200),
            ]
        elif entity_id == "alerts":
            positions = [
                (x - 350, y - 160),
                (x, y - 230),
                (x + 350, y - 160),
                (x - 350, y + 30),
                (x + 350, y + 30),
                (x, y + 240),
            ]
        elif entity_id == "log_images":
            positions = [
                (x - 350, y - 150),
                (x + 350, y - 150),
                (x - 350, y + 150),
                (x + 350, y + 150),
            ]
        else:
            raise ValueError(entity_id)

        for idx, (attr, (ax, ay)) in enumerate(zip(attrs, positions)):
            width = max(240, min(380, 120 + len(attr) * 14))
            node_id = f"{entity_id}_attr_{idx}"
            diagram.nodes.append(
                Node(
                    id=node_id,
                    kind="attribute",
                    x=ax,
                    y=ay,
                    w=width,
                    h=90,
                    label=attr,
                    font_size=22,
                )
            )
            diagram.edges.append(Edge(start=entity_id, end=node_id))

    add_entity("users", 650, 520, ENTITY_SPECS["users"]["attrs"], ENTITY_SPECS["users"]["cn"])
    add_entity("patients", 1800, 520, ENTITY_SPECS["patients"]["attrs"], ENTITY_SPECS["patients"]["cn"])
    add_entity(
        "user_patient_relation",
        1250,
        1380,
        ENTITY_SPECS["user_patient_relation"]["attrs"],
        ENTITY_SPECS["user_patient_relation"]["cn"],
    )
    add_entity("settings", 650, 2380, ENTITY_SPECS["settings"]["attrs"], ENTITY_SPECS["settings"]["cn"])
    add_entity("schedules", 3150, 560, ENTITY_SPECS["schedules"]["attrs"], ENTITY_SPECS["schedules"]["cn"])
    add_entity(
        "intake_events",
        3150,
        1860,
        ENTITY_SPECS["intake_events"]["attrs"],
        ENTITY_SPECS["intake_events"]["cn"],
    )
    add_entity("alerts", 4870, 980, ENTITY_SPECS["alerts"]["attrs"], ENTITY_SPECS["alerts"]["cn"])
    add_entity("log_images", 4850, 2520, ENTITY_SPECS["log_images"]["attrs"], ENTITY_SPECS["log_images"]["cn"])

    relationships = [
        Node("rel_owns", "relationship", 1220, 430, 220, 120, "拥有", 24),
        Node("rel_user_link", "relationship", 900, 1080, 260, 130, "关联记录", 24),
        Node("rel_patient_link", "relationship", 1600, 1080, 260, 130, "关联记录", 24),
        Node("rel_setting", "relationship", 650, 1870, 240, 120, "配置", 24),
        Node("rel_schedule", "relationship", 2470, 560, 240, 120, "制定", 24),
        Node("rel_event_patient", "relationship", 2470, 1680, 260, 130, "产生", 24),
        Node("rel_event_schedule", "relationship", 3150, 1180, 280, 130, "对应计划", 24),
        Node("rel_alert", "relationship", 4030, 980, 240, 120, "触发", 24),
        Node("rel_image", "relationship", 4010, 2300, 240, 120, "保存", 24),
    ]
    diagram.nodes.extend(relationships)

    diagram.edges.extend(
        [
            Edge("users", "rel_owns", "1", None),
            Edge("rel_owns", "patients", None, "N"),
            Edge("users", "rel_user_link", "1", None),
            Edge("rel_user_link", "user_patient_relation", None, "N"),
            Edge("patients", "rel_patient_link", "1", None),
            Edge("rel_patient_link", "user_patient_relation", None, "N"),
            Edge("users", "rel_setting", "1", None),
            Edge("rel_setting", "settings", None, "1"),
            Edge("patients", "rel_schedule", "1", None),
            Edge("rel_schedule", "schedules", None, "N"),
            Edge("patients", "rel_event_patient", "1", None),
            Edge("rel_event_patient", "intake_events", None, "N"),
            Edge("schedules", "rel_event_schedule", "1", None),
            Edge("rel_event_schedule", "intake_events", None, "N"),
            Edge("patients", "rel_alert", "1", None),
            Edge("rel_alert", "alerts", None, "N"),
            Edge("intake_events", "rel_image", "1", None),
            Edge("rel_image", "log_images", None, "N"),
        ]
    )
    return diagram


def render_diagram(diagram: Diagram) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    render_svg(diagram, OUTPUT_DIR / f"{diagram.name}.svg")
    render_png(diagram, OUTPUT_DIR / f"{diagram.name}.png")


def main() -> None:
    for entity_name, spec in ENTITY_SPECS.items():
        render_diagram(build_entity_diagram(entity_name, spec["cn"], spec["attrs"]))
    render_diagram(build_global_diagram())


if __name__ == "__main__":
    main()
