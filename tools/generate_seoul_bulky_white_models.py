from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/metrobuilder/models/psd/seoul_bulky_white"
OUT.mkdir(parents=True, exist_ok=True)


class Obj:
    def __init__(self, name: str):
        self.name = name
        self.vertices = []
        self.uvs = []
        self.faces = []

    def add_vertex(self, point):
        self.vertices.append(tuple(point))
        return len(self.vertices)

    def add_uv(self, uv):
        self.uvs.append(tuple(uv))
        return len(self.uvs)

    def quad(self, points, uvs=((0, 0), (1, 0), (1, 1), (0, 1))):
        vertex_indices = [self.add_vertex(point) for point in points]
        uv_indices = [self.add_uv(uv) for uv in uvs]
        self.faces.append([(vertex_indices[index], uv_indices[index]) for index in range(4)])

    def box(self, x0, y0, z0, x1, y1, z1):
        """Add one sharp-edged rectangular prism. Front is positive Z."""
        self.quad([(x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)])
        self.quad([(x1, y0, z0), (x0, y0, z0), (x0, y1, z0), (x1, y1, z0)])
        self.quad([(x0, y0, z0), (x0, y0, z1), (x0, y1, z1), (x0, y1, z0)])
        self.quad([(x1, y0, z1), (x1, y0, z0), (x1, y1, z0), (x1, y1, z1)])
        self.quad([(x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0)])
        self.quad([(x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1)])

    def save(self):
        output_path = OUT / f"{self.name}.obj"
        with output_path.open("w", encoding="utf-8", newline="\n") as output:
            output.write("# MetroBuilder Seoul Metro Bulky White model\n")
            output.write(f"o {self.name}\n")
            for x, y, z in self.vertices:
                output.write(f"v {x:.6f} {y:.6f} {z:.6f}\n")
            for u, v in self.uvs:
                output.write(f"vt {u:.6f} {v:.6f}\n")
            output.write("s off\n")
            for face in self.faces:
                output.write("f " + " ".join(f"{vertex}/{uv}" for vertex, uv in face) + "\n")


# All dimensions are in Minecraft blocks. The local origin is the center between
# the two door leaves. The full assembly is exactly five blocks wide and three
# blocks tall: 1.5 glass + 1 door + 1 door + 1.5 glass.
# Moving door leaves run on the rear rail. The fixed side panels occupy a
# separate front layer, leaving an unobstructed pocket for each opening leaf.
DOOR_WHITE_BACK, DOOR_WHITE_FRONT = -0.165, 0.035
DOOR_DARK_BACK, DOOR_DARK_FRONT = -0.125, 0.045
DOOR_GLASS_BACK, DOOR_GLASS_FRONT = -0.035, 0.000
DOOR_THRESHOLD_BACK, DOOR_THRESHOLD_FRONT = -0.175, 0.055

PANEL_WHITE_BACK, PANEL_WHITE_FRONT = 0.070, 0.190
PANEL_DARK_BACK, PANEL_DARK_FRONT = 0.090, 0.200
PANEL_GLASS_BACK, PANEL_GLASS_FRONT = 0.135, 0.170
PANEL_THRESHOLD_BACK, PANEL_THRESHOLD_FRONT = 0.060, 0.205

# Straight, sharp, single-row header. No rounded or chamfered corners.
model = Obj("header")
model.box(-2.5, 2.0, -0.18, 2.5, 3.0, 0.18)
model.save()

# Fixed white pieces for both 1.5-block side glass panels.
model = Obj("side_white")
for x0, x1 in [(-2.5, -2.37), (-1.13, -1.0), (1.0, 1.13), (2.37, 2.5)]:
    model.box(x0, 0.0, PANEL_WHITE_BACK, x1, 2.0, PANEL_WHITE_FRONT)
for x0, x1 in [(-2.5, -1.0), (1.0, 2.5)]:
    model.box(x0, 0.0, PANEL_WHITE_BACK, x1, 0.13, PANEL_WHITE_FRONT)
    model.box(x0, 1.87, PANEL_WHITE_BACK, x1, 2.0, PANEL_WHITE_FRONT)
model.save()

# Dark inset borders around the clean side glass.
model = Obj("side_dark")
for x0, x1 in [(-2.37, -1.13), (1.13, 2.37)]:
    border = 0.055
    model.box(x0, 0.13, PANEL_DARK_BACK, x0 + border, 1.87, PANEL_DARK_FRONT)
    model.box(x1 - border, 0.13, PANEL_DARK_BACK, x1, 1.87, PANEL_DARK_FRONT)
    model.box(x0, 0.13, PANEL_DARK_BACK, x1, 0.19, PANEL_DARK_FRONT)
    model.box(x0, 1.81, PANEL_DARK_BACK, x1, 1.87, PANEL_DARK_FRONT)
model.save()

# Clean, full-height side glass with no writing or decals.
model = Obj("side_glass")
for x0, x1 in [(-2.315, -1.185), (1.185, 2.315)]:
    model.box(x0, 0.19, PANEL_GLASS_BACK, x1, 1.81, PANEL_GLASS_FRONT)
model.save()

model = Obj("side_threshold")
for x0, x1 in [(-2.37, -1.13), (1.13, 2.37)]:
    model.box(x0, 0.02, PANEL_THRESHOLD_BACK, x1, 0.12, PANEL_THRESHOLD_FRONT)
model.save()


def make_door(side: str, x0: float, x1: float):
    frame = 0.08

    white = Obj(f"{side}_door_white")
    white.box(x0, 0.0, DOOR_WHITE_BACK, x0 + frame, 2.0, DOOR_WHITE_FRONT)
    white.box(x1 - frame, 0.0, DOOR_WHITE_BACK, x1, 2.0, DOOR_WHITE_FRONT)
    white.box(x0, 0.0, DOOR_WHITE_BACK, x1, 0.13, DOOR_WHITE_FRONT)
    white.box(x0, 1.87, DOOR_WHITE_BACK, x1, 2.0, DOOR_WHITE_FRONT)
    white.save()

    inner_x0 = x0 + frame
    inner_x1 = x1 - frame
    dark_border = 0.05

    dark = Obj(f"{side}_door_dark")
    dark.box(inner_x0, 0.13, DOOR_DARK_BACK, inner_x0 + dark_border, 1.87, DOOR_DARK_FRONT)
    dark.box(inner_x1 - dark_border, 0.13, DOOR_DARK_BACK, inner_x1, 1.87, DOOR_DARK_FRONT)
    dark.box(inner_x0, 0.13, DOOR_DARK_BACK, inner_x1, 0.19, DOOR_DARK_FRONT)
    dark.box(inner_x0, 1.81, DOOR_DARK_BACK, inner_x1, 1.87, DOOR_DARK_FRONT)
    dark.save()

    glass = Obj(f"{side}_door_glass")
    glass.box(
        inner_x0 + dark_border,
        0.19,
        DOOR_GLASS_BACK,
        inner_x1 - dark_border,
        1.81,
        DOOR_GLASS_FRONT,
    )
    glass.save()

    threshold = Obj(f"{side}_door_threshold")
    threshold.box(inner_x0, 0.02, DOOR_THRESHOLD_BACK, inner_x1, 0.12, DOOR_THRESHOLD_FRONT)
    threshold.save()


make_door("left", -1.0, 0.0)
make_door("right", 0.0, 1.0)

# Header extensions used when this pack supplies glass wings beside a native
# two-block door pack. They use the same straight/sharp housing profile.
model = Obj("header_wings")
model.box(-2.5, 2.0, -0.18, -1.0, 3.0, 0.18)
model.box(1.0, 2.0, -0.18, 2.5, 3.0, 0.18)
model.save()

print("generated", len(list(OUT.glob("*.obj"))), "OBJ files")
