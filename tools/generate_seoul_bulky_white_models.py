from pathlib import Path
import math

OUT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/metrobuilder/models/psd/seoul_bulky_white'
OUT.mkdir(parents=True, exist_ok=True)

class Obj:
    def __init__(self, name):
        self.name=name
        self.v=[]; self.vt=[]; self.faces=[]
    def add_vertex(self,p): self.v.append(tuple(p)); return len(self.v)
    def add_uv(self,uv): self.vt.append(tuple(uv)); return len(self.vt)
    def quad(self, pts, uvs=((0,0),(1,0),(1,1),(0,1))):
        vi=[self.add_vertex(p) for p in pts]
        ti=[self.add_uv(uv) for uv in uvs]
        self.faces.append([(vi[i],ti[i]) for i in range(4)])
    def box(self,x0,y0,z0,x1,y1,z1):
        # outward winding; front is +Z
        self.quad([(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]) # front
        self.quad([(x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0)]) # back
        self.quad([(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)]) # left
        self.quad([(x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1)]) # right
        self.quad([(x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)]) # top
        self.quad([(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)]) # bottom
    def chamfered_prism(self,x0,y0,z0,x1,y1,z1,c):
        c=max(0,min(c,(x1-x0)/2,(y1-y0)/2))
        ring=[(x0+c,y0),(x1-c,y0),(x1,y0+c),(x1,y1-c),(x1-c,y1),(x0+c,y1),(x0,y1-c),(x0,y0+c)]
        # front/back ngon triangulated fan via quads/triangles faces entries
        f=[self.add_vertex((x,y,z1)) for x,y in ring]
        b=[self.add_vertex((x,y,z0)) for x,y in ring]
        # one UV per vertex around approximate perimeter; face UVs normalized xy
        fuv=[self.add_uv(((x-x0)/(x1-x0),(y-y0)/(y1-y0))) for x,y in ring]
        buv=[self.add_uv(((x-x0)/(x1-x0),(y-y0)/(y1-y0))) for x,y in ring]
        for i in range(1,len(ring)-1):
            self.faces.append([(f[0],fuv[0]),(f[i],fuv[i]),(f[i+1],fuv[i+1])])
            self.faces.append([(b[0],buv[0]),(b[i+1],buv[i+1]),(b[i],buv[i])])
        for i in range(len(ring)):
            j=(i+1)%len(ring)
            t=[self.add_uv((0,0)),self.add_uv((1,0)),self.add_uv((1,1)),self.add_uv((0,1))]
            self.faces.append([(b[i],t[0]),(b[j],t[1]),(f[j],t[2]),(f[i],t[3])])
    def save(self):
        p=OUT/f'{self.name}.obj'
        with p.open('w',encoding='utf-8',newline='\n') as f:
            f.write('# MetroBuilder Seoul Metro Bulky White model\n')
            f.write(f'o {self.name}\n')
            for x,y,z in self.v: f.write(f'v {x:.6f} {y:.6f} {z:.6f}\n')
            for u,v in self.vt: f.write(f'vt {u:.6f} {v:.6f}\n')
            f.write('s 1\n')
            for face in self.faces:
                f.write('f '+' '.join(f'{vi}/{ti}' for vi,ti in face)+'\n')

# Geometry constants
front=0.145; back=-0.145
white_front=0.155; white_back=-0.155
dark_front=0.170; dark_back=-0.110
glass_front=0.125; glass_back=0.092
threshold_front=0.175; threshold_back=-0.165

# Header: bulky, chamfered housing exactly 5 blocks wide and one block tall
m=Obj('header')
m.chamfered_prism(-2.5,2.0,-0.20,2.5,3.0,0.20,0.055)
# subtle lower lip (not decorative aluminum; same painted housing material)
m.chamfered_prism(-2.5,1.94,-0.185,2.5,2.08,0.185,0.025)
m.save()

# Fixed white pieces for the two clean side glass bays
m=Obj('side_white')
for x0,x1 in [(-2.5,-2.36),(-1.14,-1.0),(1.0,1.14),(2.36,2.5)]:
    m.chamfered_prism(x0,0.0,white_back,x1,2.0,white_front,0.018)
for x0,x1 in [(-2.5,-1.0),(1.0,2.5)]:
    m.chamfered_prism(x0,0.0,white_back,x1,0.13,white_front,0.018)
    m.chamfered_prism(x0,1.87,white_back,x1,2.0,white_front,0.018)
m.save()

m=Obj('side_dark')
for x0,x1 in [(-2.36,-1.14),(1.14,2.36)]:
    # inner dark border
    m.box(x0,0.13,dark_back,x0+0.055,1.87,dark_front)
    m.box(x1-0.055,0.13,dark_back,x1,1.87,dark_front)
    m.box(x0,0.13,dark_back,x1,0.19,dark_front)
    m.box(x0,1.81,dark_back,x1,1.87,dark_front)
m.save()

m=Obj('side_glass')
for x0,x1 in [(-2.305,-1.195),(1.195,2.305)]:
    m.box(x0,0.19,glass_back,x1,1.81,glass_front)
m.save()

m=Obj('side_threshold')
for x0,x1 in [(-2.36,-1.14),(1.14,2.36)]:
    m.chamfered_prism(x0,0.02,threshold_back,x1,0.12,threshold_front,0.012)
m.save()

# Door leaf generator: local positions are already in assembly coordinates when closed
def make_door(side, x0, x1):
    # painted outer frame
    m=Obj(f'{side}_door_white')
    fw=0.075
    m.chamfered_prism(x0,0.0,white_back,x0+fw,2.0,white_front,0.012)
    m.chamfered_prism(x1-fw,0.0,white_back,x1,2.0,white_front,0.012)
    m.chamfered_prism(x0,0.0,white_back,x1,0.13,white_front,0.012)
    m.chamfered_prism(x0,1.87,white_back,x1,2.0,white_front,0.012)
    m.save()
    # black inset frame
    m=Obj(f'{side}_door_dark')
    ix0=x0+fw; ix1=x1-fw
    m.box(ix0,0.13,dark_back,ix0+0.05,1.87,dark_front)
    m.box(ix1-0.05,0.13,dark_back,ix1,1.87,dark_front)
    m.box(ix0,0.13,dark_back,ix1,0.19,dark_front)
    m.box(ix0,1.81,dark_back,ix1,1.87,dark_front)
    m.save()
    # glass
    m=Obj(f'{side}_door_glass')
    m.box(ix0+0.05,0.19,glass_back,ix1-0.05,1.81,glass_front)
    m.save()
    # threshold
    m=Obj(f'{side}_door_threshold')
    m.chamfered_prism(x0+fw,0.02,threshold_back,x1-fw,0.12,threshold_front,0.01)
    m.save()

make_door('left',-1.0,0.0)
make_door('right',0.0,1.0)

# Companion header wings used with native TJMetro doors
m=Obj('header_wings')
m.chamfered_prism(-2.5,2.0,-0.20,-1.0,3.0,0.20,0.055)
m.chamfered_prism(1.0,2.0,-0.20,2.5,3.0,0.20,0.055)
m.save()

print('generated', len(list(OUT.glob('*.obj'))), 'OBJ files')
