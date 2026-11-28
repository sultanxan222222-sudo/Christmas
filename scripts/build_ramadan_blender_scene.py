import bpy
import math
import os
import random
from mathutils import Vector, Matrix

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MODELS = os.path.join(ROOT, "app", "src", "main", "assets", "models")
OUT = os.path.join(MODELS, "ramadan_dream_scene.glb")

# Start from a clean scene; all geometry remains real mesh/curve/text data.
bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
for datablocks in (bpy.data.meshes, bpy.data.curves, bpy.data.materials, bpy.data.actions):
    for block in list(datablocks):
        if block.users == 0:
            datablocks.remove(block)

def import_group(filename, label, scale, location):
    before = set(bpy.context.scene.objects)
    bpy.ops.import_scene.gltf(filepath=os.path.join(MODELS, filename))
    imported = [o for o in bpy.context.scene.objects if o not in before]
    root = bpy.data.objects.new(label + " • scene transform", None)
    bpy.context.scene.collection.objects.link(root)
    for obj in imported:
        world = obj.matrix_world.copy()
        obj.parent = root
        obj.matrix_parent_inverse = Matrix.Identity(4)
        obj.matrix_basis = world
    root.location = location
    root.scale = (scale, scale, scale)
    return root, imported

def pbr_material(name, color, metallic=0.0, roughness=0.35, emission=None):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1.0)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1.0)
    bsdf.inputs["Metallic"].default_value = metallic
    bsdf.inputs["Roughness"].default_value = roughness
    if emission:
        if "Emission Color" in bsdf.inputs:
            bsdf.inputs["Emission Color"].default_value = (*emission, 1.0)
            bsdf.inputs["Emission Strength"].default_value = 3.0
        elif "Emission" in bsdf.inputs:
            bsdf.inputs["Emission"].default_value = (*emission, 1.0)
    return mat

def tint_mosque(objects):
    # Force the architecture into the intended rich emerald palette.
    # The source GLB uses textured material graphs that some glTF exporters
    # flatten incorrectly; use explicit PBR Principled factors for reliable color.
    palette = {
        "Limestone ashlar": ((0.018, 0.235, 0.135, 1.0), 0.24, 0.31),
        "Lead sheet": ((0.006, 0.105, 0.062, 1.0), 0.52, 0.23),
        "Dressed limestone": ((0.12, 0.39, 0.255, 1.0), 0.48, 0.24),
        "Dark interior": ((0.003, 0.018, 0.014, 1.0), 0.12, 0.22),
        "Wrought iron": ((0.018, 0.075, 0.052, 1.0), 0.72, 0.25),
        "Oak boarded door": ((0.095, 0.19, 0.105, 1.0), 0.22, 0.34),
        "Gilded metal": ((0.72, 0.43, 0.12, 1.0), 0.82, 0.2),
        "Window glass": ((0.006, 0.035, 0.026, 1.0), 0.28, 0.14),
        "Leaded quarry glass": ((0.012, 0.09, 0.06, 1.0), 0.35, 0.18),
        "White marble": ((0.16, 0.36, 0.24, 1.0), 0.35, 0.23),
        "Glazed geometric tiles": ((0.012, 0.24, 0.14, 1.0), 0.38, 0.2),
    }
    for obj in objects:
        if obj.type != "MESH":
            continue
        for slot_index, mat in enumerate(list(obj.data.materials)):
            if not mat:
                continue
            matched = next((v for key, v in palette.items() if key in mat.name), None)
            if not matched:
                continue
            color, metallic, roughness = matched
            # Make a dedicated material per source slot so all parts retain
            # separate PBR response while avoiding unsupported texture mixes.
            emerald_mat = mat.copy()
            emerald_mat.name = "Ramadan Emerald • " + mat.name
            emerald_mat.use_nodes = True
            nodes = emerald_mat.node_tree.nodes
            bsdf = nodes.get("Principled BSDF")
            if bsdf:
                base = bsdf.inputs.get("Base Color")
                if base:
                    for link in list(base.links):
                        emerald_mat.node_tree.links.remove(link)
                    base.default_value = color
                bsdf.inputs["Metallic"].default_value = metallic
                bsdf.inputs["Roughness"].default_value = roughness
                if "Coat Weight" in bsdf.inputs:
                    bsdf.inputs["Coat Weight"].default_value = 0.22
            obj.data.materials[slot_index] = emerald_mat

# Imported high-detail licensed source models.
mosque_root, mosque_objects = import_group(
    "mosque_two_minarets.glb", "Emerald Ottoman mosque", 0.073, (0.0, 0.0, 0.0)
)
tint_mosque(mosque_objects)

# Hand-built architectural facade layer: custom meshwork authored in Blender
# to enrich the imported massing with a coherent, inspectable 3D entrance.
def add_box(name, location, scale, material, bevel=0.0):
    bpy.ops.mesh.primitive_cube_add(size=1, location=location)
    obj=bpy.context.object
    obj.name=name
    obj.dimensions=scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    obj.data.materials.append(material)
    if bevel:
        mod=obj.modifiers.new("Hand-finished stone edges","BEVEL")
        mod.width=bevel
        mod.segments=3
        obj.modifiers.new("Facade weighted normals","WEIGHTED_NORMAL")
    return obj

def add_arch_outline(name, cx, y, base_z, width, spring_z, crown_z, material, thickness=0.018):
    # A true 3D extruded curve following the horseshoe-like pointed Islamic arch.
    points=[(-width/2,base_z),(-width/2,spring_z)]
    half=width/2
    for i in range(25):
        t=i/24
        x=-half+2*half*t
        z=spring_z+(crown_z-spring_z)*math.sin(math.pi*t)**0.78
        points.append((x,z))
    points.append((width/2,base_z))
    curve=bpy.data.curves.new(name+" • carved profile","CURVE")
    curve.dimensions="3D"
    curve.bevel_depth=thickness
    curve.bevel_resolution=4
    spline=curve.splines.new("POLY")
    spline.points.add(len(points)-1)
    for p,(x,z) in zip(spline.points,points):
        p.co=(cx+x,y,z,1)
    obj=bpy.data.objects.new(name,curve)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(material)
    return obj

# Main entrance: recessed double door, layered pointed arch, pilasters and gold archivolt.
portal_y=-2.035
add_box("Custom mosque • recessed emerald double door",(0,portal_y,0.57),(0.70,0.055,0.94),emerald,0.025)
add_box("Custom mosque • door central seam",(0,portal_y-0.034,0.57),(0.012,0.012,0.88),gold,0.003)
for side in (-1,1):
    x=side*0.405
    add_box("Custom mosque • carved entrance pilaster",(x,portal_y-0.025,0.60),(0.105,0.09,1.18),gold,0.018)
    add_box("Custom mosque • pilaster emerald inset",(x,portal_y-0.078,0.61),(0.052,0.012,0.91),emerald,0.008)
    for z in (0.18,0.36,0.88,1.08):
        add_box("Custom mosque • pilaster gold collar",(x,portal_y-0.09,z),(0.13,0.025,0.025),gold_light,0.006)
add_arch_outline("Custom mosque • outer gold pointed archivolt",0,portal_y-0.085,0.08,0.98,0.83,1.38,gold_light,0.026)
add_arch_outline("Custom mosque • inner emerald arch reveal",0,portal_y-0.095,0.13,0.78,0.78,1.25,emerald,0.018)
add_arch_outline("Custom mosque • inner gilded pinstripe",0,portal_y-0.112,0.17,0.69,0.76,1.19,gold,0.009)
# Door panels and eight raised brass studs provide actual relief, not a flat texture.
for side in (-1,1):
    add_box("Custom mosque • carved door panel",(side*0.17,portal_y-0.045,0.59),(0.22,0.018,0.65),gold,0.012)
    add_box("Custom mosque • inset door panel",(side*0.17,portal_y-0.058,0.59),(0.19,0.012,0.62),emerald,0.01)
    for z in (0.36,0.59,0.82):
        bpy.ops.mesh.primitive_uv_sphere_add(segments=16,ring_count=8,radius=0.018,location=(side*0.17,portal_y-0.078,z))
        stud=bpy.context.object
        stud.name="Custom mosque • brass door rosette"
        stud.scale=(1.0,0.35,1.0)
        stud.data.materials.append(gold_light)
# Small geometric star medallions flanking the portal.
for x in (-0.62,0.62):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24,ring_count=12,radius=0.075,location=(x,portal_y-0.06,0.82))
    med=bpy.context.object
    med.name="Custom mosque • facade medallion"
    med.scale=(1.0,0.22,1.0)
    med.data.materials.append(gold)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16,ring_count=8,radius=0.026,location=(x,portal_y-0.085,0.82))
    center=bpy.context.object
    center.name="Custom mosque • medallion center"
    center.scale=(1.0,0.25,1.0)
    center.data.materials.append(gold_light)

# Two mature broad-canopy trees frame the courtyard and physically anchor the banner rope.
tree_root, tree_objects = import_group(
    "broadleaf_canopy_tree.glb", "Blessed-tree-inspired left broad canopy", 0.145, (-2.42, -0.48, 0.0)
)
tree_right_root, tree_right_objects = import_group(
    "broadleaf_canopy_tree.glb", "Right banner-anchor broad canopy", 0.145, (2.42, -0.48, 0.0)
)
# Improve foliage readability while retaining the source meshes and textures.
for tree_obj in tree_objects + tree_right_objects:
    if tree_obj.type == "MESH":
        for tree_mat in tree_obj.data.materials:
            if tree_mat and tree_mat.use_nodes:
                tree_bsdf = tree_mat.node_tree.nodes.get("Principled BSDF")
                if tree_bsdf:
                    tree_base = tree_bsdf.inputs.get("Base Color")
                    if tree_base and not tree_base.is_linked:
                        rgba = list(tree_base.default_value)
                        tree_base.default_value = (min(1.0, rgba[0] * 1.65 + 0.025),
                                                   min(1.0, rgba[1] * 1.65 + 0.025),
                                                   min(1.0, rgba[2] * 1.65 + 0.025), rgba[3])
                    tree_rough = tree_bsdf.inputs.get("Roughness")
                    if tree_rough:
                        tree_rough.default_value = 0.72
cat_root, cat_objects = import_group(
    "sleeping_cat.glb", "Sleeping kitten • clearly visible hero animal", 2.95, (-1.86, -1.34, 0.02)
)
lantern_left_root, lantern_left_objects = import_group(
    "brass_hurricane_lantern.glb", "Left brass lantern", 3.65, (-0.98, -2.25, 0.0)
)
lantern_right_root, lantern_right_objects = import_group(
    "brass_hurricane_lantern.glb", "Right brass lantern", 3.65, (0.98, -2.25, 0.0)
)

# Dense, layered broadleaf foliage overlay with curved individual leaves.
def add_realistic_leaf_canopy(cx, cy, seed):
    rng = random.Random(seed)
    leaf_mats = [
        pbr_material("Foliage • deep emerald", (0.018, 0.105, 0.045), 0.0, 0.82),
        pbr_material("Foliage • forest green", (0.035, 0.19, 0.072), 0.0, 0.78),
        pbr_material("Foliage • fresh leaf", (0.075, 0.27, 0.105), 0.0, 0.74),
        pbr_material("Foliage • moonlit leaf", (0.12, 0.31, 0.14), 0.0, 0.72),
    ]
    verts, faces, material_ids = [], [], []
    clusters = [(-0.48,0.00,2.28,0.68,0.56,0.48),(0.00,0.00,2.52,0.76,0.62,0.52),
                (0.48,0.02,2.30,0.68,0.55,0.48),(-0.22,0.08,2.83,0.60,0.48,0.42),
                (0.30,0.06,2.84,0.60,0.48,0.42),(0.00,-0.34,2.20,0.68,0.48,0.40)]
    for ox,oy,oz,rx,ry,rz in clusters:
        for _ in range(190):
            while True:
                dx,dy,dz=(rng.uniform(-1,1) for _ in range(3))
                if dx*dx+dy*dy+dz*dz <= 1: break
            px,py,pz=cx+ox+dx*rx,cy+oy+dy*ry,oz+dz*rz
            length=rng.uniform(0.105,0.19)
            width=length*rng.uniform(0.28,0.48)
            angle=rng.uniform(0,math.tau)
            ux,uy=math.cos(angle),math.sin(angle)
            vx,vy=-uy,ux
            base=len(verts)
            verts.extend([(px-ux*length*0.5,py-uy*length*0.5,pz),
                (px-vx*width*0.5,py-vy*width*0.5,pz+0.012),
                (px+ux*length*0.12-vx*width*0.5,py+uy*length*0.12-vy*width*0.5,pz+0.025),
                (px+ux*length*0.12+vx*width*0.5,py+uy*length*0.12+vy*width*0.5,pz+0.025),
                (px+vx*width*0.5,py+vy*width*0.5,pz+0.012),
                (px+ux*length*0.5,py+uy*length*0.5,pz+0.004)])
            faces.extend([(base,base+1,base+2),(base,base+2,base+3),
                          (base,base+3,base+4),(base,base+4,base+5)])
            mi=rng.choices(range(4),[24,42,27,7])[0]
            material_ids.extend([mi]*4)
    mesh=bpy.data.meshes.new("Layered broadleaf canopy • individual leaves")
    mesh.from_pydata(verts,[],faces)
    for mat in leaf_mats: mesh.materials.append(mat)
    mesh.update()
    obj=bpy.data.objects.new("Dense realistic foliage overlay",mesh)
    bpy.context.collection.objects.link(obj)
    for poly,mi in zip(mesh.polygons,material_ids):
        poly.material_index=mi
        poly.use_smooth=True

add_realistic_leaf_canopy(-2.42,-0.48,1201)
add_realistic_leaf_canopy(2.42,-0.48,1202)

# Warm gold, emerald textile and night-ground materials.
gold = pbr_material("Ramadan • brushed antique gold", (0.72, 0.39, 0.095), 0.82, 0.22)
gold_light = pbr_material("Ramadan • engraved gold", (0.94, 0.68, 0.25), 0.72, 0.2)
emerald = pbr_material("Ramadan • deep emerald silk", (0.012, 0.18, 0.105), 0.08, 0.48)
night_ground = pbr_material("Ramadan • midnight garden stone", (0.012, 0.035, 0.029), 0.18, 0.34)
flame_mat = pbr_material("Ramadan • living amber flame", (1.0, 0.19, 0.018), 0.0, 0.25, (1.0, 0.13, 0.008))

# A softly reflective, hero-sized courtyard plane; it must not dominate Filament auto-framing.
bpy.ops.mesh.primitive_plane_add(size=5.8, location=(0, 0, -0.018))
ground = bpy.context.object
ground.name = "Midnight courtyard • ground"
ground.data.materials.append(night_ground)

# A physically modelled rope and seven cloth pennants, one letter per pennant.
curve = bpy.data.curves.new("Gold rope • rounded cord", "CURVE")
curve.dimensions = "3D"
curve.bevel_depth = 0.012
curve.bevel_resolution = 4
spline = curve.splines.new("BEZIER")
spline.bezier_points.add(2)
for bp, co in zip(spline.bezier_points, [(-2.42, -1.12, 2.82), (0, -1.12, 2.60), (2.42, -1.12, 2.82)]):
    bp.co = co
    bp.handle_left_type = "AUTO"
    bp.handle_right_type = "AUTO"
rope = bpy.data.objects.new("Gold rope • Ramadan banners", curve)
bpy.context.collection.objects.link(rope)
rope.data.materials.append(gold)

def make_flag(index, letter, x):
    width, height = 0.43, 0.49
    nx, nz = 12, 14
    verts, faces = [], []
    for iz in range(nz + 1):
        v = iz / nz
        for ix in range(nx + 1):
            u = ix / nx
            # Real cloth curvature and a gently scalloped lower edge.
            wave = 0.055 * math.sin(u * math.pi * 2.2 + index * 0.7) * v
            sag = 0.055 * math.sin(u * math.pi)
            z = 2.47 - v * height + sag * v
            verts.append((x + (u - 0.5) * width, -1.13 + wave, z))
    for iz in range(nz):
        for ix in range(nx):
            a = iz * (nx + 1) + ix
            faces.append((a, a + 1, a + nx + 2, a + nx + 1))
    mesh = bpy.data.meshes.new("Silk weave mesh")
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new("Emerald silk pennant %02d" % index, mesh)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(emerald)
    solid = obj.modifiers.new("Silk thickness", "SOLIDIFY")
    solid.thickness = 0.006
    bevel = obj.modifiers.new("Soft cloth edges", "BEVEL")
    bevel.width = 0.008
    bevel.segments = 2
    # Subtle cloth sway around its suspension point.
    obj.keyframe_insert(data_path="rotation_euler", frame=1)
    obj.rotation_euler.y = math.radians(3.0)
    obj.keyframe_insert(data_path="rotation_euler", frame=20)
    obj.rotation_euler.y = math.radians(-3.0)
    obj.keyframe_insert(data_path="rotation_euler", frame=40)
    obj.rotation_euler.y = math.radians(3.0)
    obj.keyframe_insert(data_path="rotation_euler", frame=60)
    text_curve = bpy.data.curves.new("Gold Ramadan lettering", "FONT")
    text_curve.body = letter
    text_curve.align_x = "CENTER"
    text_curve.align_y = "CENTER"
    text_curve.size = 0.285
    text_curve.extrude = 0.004
    text_curve.bevel_depth = 0.0015
    text_obj = bpy.data.objects.new("Gold lettering • " + letter, text_curve)
    bpy.context.collection.objects.link(text_obj)
    text_obj.location = (x, -1.165, 2.47 - height * 0.50)
    text_obj.rotation_euler = (math.radians(90), 0, 0)
    text_obj.data.materials.append(gold_light)

for i, letter in enumerate("РАМАДАН"):
    make_flag(i, letter, -1.50 + i * 0.50)

# Extruded crescent mesh and a five-point star: actual 3D objects, not sprites.
def extruded_polygon(name, points, depth, material, location):
    count = len(points)
    verts = [(x, y, -depth/2) for x, y in points] + [(x, y, depth/2) for x, y in points]
    faces = [tuple(range(count-1, -1, -1)), tuple(range(count, count*2))]
    for i in range(count):
        j = (i + 1) % count
        faces.append((i, j, j + count, i + count))
    mesh = bpy.data.meshes.new(name + " • solid mesh")
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    obj.location = location
    obj.data.materials.append(material)
    bevel = obj.modifiers.new("Polished rounded edges", "BEVEL")
    bevel.width = 0.025
    bevel.segments = 4
    obj.modifiers.new("Weighted corner normals", "WEIGHTED_NORMAL")
    return obj

outer_r, inner_r, offset = 0.38, 0.32, 0.17
crescent_points = []
for i in range(33):
    a = math.radians(55 + i * (250/32))
    crescent_points.append((outer_r * math.cos(a), outer_r * math.sin(a)))
for i in range(32, -1, -1):
    a = math.radians(55 + i * (250/32))
    x = offset + inner_r * math.cos(a)
    y = inner_r * math.sin(a)
    crescent_points.append((x, y))
moon = extruded_polygon("Golden crescent moon", crescent_points, 0.09, gold_light, (2.05, -0.3, 2.12))
moon.rotation_euler.x = math.radians(90)

star_points = []
for i in range(10):
    a = math.pi / 2 + i * math.pi / 5
    r = 0.19 if i % 2 == 0 else 0.082
    star_points.append((r * math.cos(a), r * math.sin(a)))
star = extruded_polygon("Golden five-point star", star_points, 0.07, gold_light, (2.56, -0.3, 2.30))
star.rotation_euler.x = math.radians(90)

# Hand-finished 3D brass detailing around both lantern glass chambers:
# engraved collars, eight structural ribs, a crown and a small finial.
def decorate_lantern(cx, cy):
    for z, radius, minor in [(0.15, 0.125, 0.009), (0.34, 0.125, 0.009),
                             (0.18, 0.105, 0.004), (0.31, 0.105, 0.004)]:
        bpy.ops.mesh.primitive_torus_add(
            major_segments=48, minor_segments=10,
            major_radius=radius, minor_radius=minor,
            location=(cx, cy, z)
        )
        ring = bpy.context.object
        ring.name = "Lantern • turned brass collar"
        ring.data.materials.append(gold)
    for i in range(8):
        angle = 2 * math.pi * i / 8
        x = cx + 0.112 * math.cos(angle)
        y = cy + 0.112 * math.sin(angle)
        bpy.ops.mesh.primitive_cylinder_add(
            vertices=12, radius=0.0045, depth=0.155,
            location=(x, y, 0.245)
        )
        rib = bpy.context.object
        rib.name = "Lantern • brass glass guard rib"
        rib.data.materials.append(gold_light)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=12, radius=0.025,
                                         location=(cx, cy, 0.365))
    crown = bpy.context.object
    crown.name = "Lantern • polished crown"
    crown.scale = (1.5, 1.5, 0.65)
    crown.data.materials.append(gold)
    bpy.ops.mesh.primitive_cone_add(vertices=24, radius1=0.018, radius2=0.002,
                                    depth=0.045, location=(cx, cy, 0.405))
    finial = bpy.context.object
    finial.name = "Lantern • pointed finial"
    finial.data.materials.append(gold_light)

decorate_lantern(-0.98, -2.25)
decorate_lantern(0.98, -2.25)

# Real flame meshes and warm point lights inside the two glass lanterns.
def create_flame(name, x, y):
    # The source lantern glass occupies roughly z=0.16..0.34 after scaling.
    # Keep the whole flame inside that glass envelope instead of above the roof.
    levels = [(0.00, 0.035), (0.055, 0.032), (0.12, 0.022), (0.19, 0.010), (0.25, 0.0)]
    segments = 32
    verts, faces = [], []
    for z, radius in levels:
        for i in range(segments):
            a = 2 * math.pi * i / segments
            shift = 0.009 * (z / 0.15) ** 2
            verts.append((x + shift + radius * math.cos(a), y + radius * math.sin(a), z))
    for level in range(len(levels)-1):
        for i in range(segments):
            a = level * segments + i
            b = level * segments + (i + 1) % segments
            faces.append((a, b, b + segments, a + segments))
    mesh = bpy.data.meshes.new(name + " • flame volume")
    mesh.from_pydata(verts, [], faces)
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    obj.data.materials.append(flame_mat)
    obj.location.z = 0.18
    obj.scale = (1, 1, 1)
    for frame, sx, sy, sz in [(1, 1, 1, 1), (12, 0.94, 0.94, 1.06), (24, 1.04, 1.03, 0.96), (36, 0.96, 0.97, 1.04), (48, 1, 1, 1)]:
        obj.scale = (sx, sy, sz)
        obj.keyframe_insert(data_path="scale", frame=frame)
    light_data = bpy.data.lights.new(name + " • warm glow", "POINT")
    light_data.energy = 7
    light_data.color = (1.0, 0.32, 0.075)
    light_data.shadow_soft_size = 0.18
    light = bpy.data.objects.new(name + " • warm glow", light_data)
    bpy.context.collection.objects.link(light)
    light.location = (x, y, 0.255)
    for frame, energy in [(1, 7), (12, 9), (24, 6), (36, 8), (48, 7)]:
        light_data.energy = energy
        light_data.keyframe_insert(data_path="energy", frame=frame)
    return obj

create_flame("Left lantern • living flame", -0.98, -2.25)
create_flame("Right lantern • living flame", 0.98, -2.25)

# Hidden-at-night warm point lights at prayer-hall windows and minaret balconies.
# Filament toggles these by their stable node-name prefix at each prayer time.
prayer_light_positions = [
    (-0.72, -1.96, 0.42), (-0.36, -1.96, 0.58), (0.0, -1.96, 0.66),
    (0.36, -1.96, 0.58), (0.72, -1.96, 0.42),
    (-0.96, 0.08, 1.52), (0.96, 0.08, 1.52),
]
for index, position in enumerate(prayer_light_positions):
    light_data = bpy.data.lights.new("Prayer warm window light %02d" % index, "POINT")
    light_data.energy = 1.0
    light_data.color = (1.0, 0.58, 0.28)
    light_data.shadow_soft_size = 0.20
    light_obj = bpy.data.objects.new("PRAYER_WINDOW_LIGHT_%02d" % index, light_data)
    bpy.context.collection.objects.link(light_obj)
    light_obj.location = position

# A field of tiny emissive 3D stars behind the architecture.
star_mat = pbr_material("Ramadan • cool starlight", (0.48, 0.72, 0.88), 0.0, 0.3, (0.18, 0.34, 0.52))
star_rng = random.Random(20261127)
for index in range(42):
    radius = star_rng.uniform(0.008, 0.019)
    x = star_rng.uniform(-2.25, 2.25)
    y = star_rng.uniform(1.35, 2.95)
    z = star_rng.uniform(2.65, 4.0)
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=radius, location=(x, y, z))
    star_obj = bpy.context.object
    star_obj.name = "Distant 3D star %02d" % index
    star_obj.data.materials.append(star_mat)

# A subtle breathing motion for the sleeping kitten.
for frame, factor in [(1, 1.0), (30, 1.018), (60, 1.0)]:
    cat_root.scale = (2.95 * factor, 2.95, 2.95 * factor)
    cat_root.keyframe_insert(data_path="scale", frame=frame)

# Keep the scene in a gentle looping animation range.
scene = bpy.context.scene
scene.frame_start = 1
scene.frame_end = 60
scene.render.fps = 30
scene.world.color = (0.002, 0.004, 0.012)
scene.world.use_nodes = True
world_bg = scene.world.node_tree.nodes.get("Background")
world_bg.inputs["Color"].default_value = (0.0015, 0.004, 0.018, 1.0)
world_bg.inputs["Strength"].default_value = 0.055
scene.view_settings.view_transform = "AgX"
scene.view_settings.look = "AgX - Medium High Contrast"
scene.view_settings.exposure = 0.15
scene.view_settings.gamma = 1.0
scene.render.resolution_x = 1920
scene.render.resolution_y = 1080
scene.render.resolution_percentage = 100

# Render a real 3D proof image in CI to catch framing and material defects before APK delivery.
scene = bpy.context.scene
available_engines = {item.identifier for item in scene.render.bl_rna.properties["engine"].enum_items}
if "BLENDER_EEVEE_NEXT" in available_engines:
    scene.render.engine = "BLENDER_EEVEE_NEXT"
elif "BLENDER_EEVEE" in available_engines:
    scene.render.engine = "BLENDER_EEVEE"
camera_data = bpy.data.cameras.new("Ramadan hero preview camera")
camera = bpy.data.objects.new("Ramadan hero preview camera", camera_data)
scene.collection.objects.link(camera)
camera.location = (3.1, -11.8, 4.9)
target = Vector((0.0, -0.15, 1.48))
camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()
camera_data.lens = 48
scene.camera = camera
key_data = bpy.data.lights.new("Preview softbox key", "AREA")
key_data.energy = 1900
key_data.shape = "DISK"
key_data.size = 7.0
key_obj = bpy.data.objects.new("Preview softbox key", key_data)
scene.collection.objects.link(key_obj)
key_obj.location = (-5.0, -3.0, 8.5)
# Warm frontal fill lifts the mosque facade, kitten and brass lanterns out of shadow.
fill_data = bpy.data.lights.new("Warm courtyard fill", "AREA")
fill_data.energy = 950
fill_data.color = (1.0, 0.72, 0.46)
fill_data.shape = "DISK"
fill_data.size = 5.0
fill_obj = bpy.data.objects.new("Warm courtyard fill", fill_data)
scene.collection.objects.link(fill_obj)
fill_obj.location = (0.0, -6.0, 3.6)
fill_obj.rotation_euler = (Vector((0.0, 0.0, 1.2)) - fill_obj.location).to_track_quat("-Z", "Y").to_euler()
key_obj.rotation_euler = (Vector((0.0, 0.0, 1.0)) - key_obj.location).to_track_quat("-Z", "Y").to_euler()
scene.render.resolution_x = 1920
scene.render.resolution_y = 1080
scene.render.resolution_percentage = 90
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = os.path.join(ROOT, "ramadan-3d-preview.png")
moon_data = bpy.data.lights.new("Moonlight • cool cinematic rim", "AREA")
moon_data.energy = 1250
moon_data.color = (0.28, 0.48, 0.82)
moon_data.shape = "DISK"
moon_data.size = 8.0
moon_obj = bpy.data.objects.new("Moonlight • cool cinematic rim", moon_data)
scene.collection.objects.link(moon_obj)
moon_obj.location = (4.5, 3.0, 7.5)
moon_obj.rotation_euler = (Vector((0.0, 0.0, 1.0)) - moon_obj.location).to_track_quat("-Z", "Y").to_euler()
fog_mat = bpy.data.materials.new("Night atmosphere • subtle volumetric haze")
fog_mat.use_nodes = True
fog_nodes = fog_mat.node_tree.nodes
fog_nodes.clear()
out_node = fog_nodes.new("ShaderNodeOutputMaterial")
volume_node = fog_nodes.new("ShaderNodeVolumePrincipled")
volume_node.inputs["Color"].default_value = (0.16, 0.23, 0.34, 1.0)
volume_node.inputs["Density"].default_value = 0.012
fog_mat.node_tree.links.new(volume_node.outputs["Volume"], out_node.inputs["Volume"])
bpy.ops.mesh.primitive_cube_add(size=200, location=(0, 0, 0))
fog_box = bpy.context.object
fog_box.name = "Cinematic atmospheric volume"
fog_box.data.materials.append(fog_mat)
if hasattr(scene, "eevee") and hasattr(scene.eevee, "taa_render_samples"):
    scene.eevee.taa_render_samples = 32
bpy.ops.render.render(write_still=True)
print("Rendered 3D scene preview:", scene.render.filepath)

# The 200-unit fog volume is preview-only. Exporting it would make Filament's
# transformToUnitCube fit a huge invisible box and shrink every visible asset.
bpy.data.objects.remove(fog_box, do_unlink=True)

# Select every scene object for a complete self-contained GLB export.
bpy.ops.object.select_all(action="SELECT")
bpy.context.view_layer.objects.active = mosque_root
bpy.ops.export_scene.gltf(
    filepath=OUT,
    export_format="GLB",
    export_animations=True,
    export_skins=True,
    export_materials="EXPORT",
    export_image_format="AUTO",
    export_apply=True,
    export_cameras=False,
    export_lights=True,
)
print("Created Ramadan 3D scene:", OUT, os.path.getsize(OUT), "bytes")
