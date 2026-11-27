import bpy
import math
import os
from mathutils import Vector

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
        obj.matrix_world = world
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
    targets = {
        "Cut stone, warm limestone": (0.025, 0.22, 0.13, 1.0),
        "Lead sheet domes and roofs": (0.018, 0.16, 0.095, 1.0),
        "Stone trims and cornices, light": (0.12, 0.42, 0.27, 1.0),
    }
    for obj in objects:
        if obj.type != "MESH":
            continue
        for mat in obj.data.materials:
            if not mat or mat.name not in targets or not mat.use_nodes:
                continue
            bsdf = mat.node_tree.nodes.get("Principled BSDF")
            if not bsdf:
                continue
            # Keep source texture detail and multiply it by an emerald tint.
            base = bsdf.inputs.get("Base Color")
            if base and base.is_linked:
                source = base.links[0].from_socket
                multiply = mat.node_tree.nodes.new("ShaderNodeMixRGB")
                multiply.blend_type = "MULTIPLY"
                multiply.inputs[0].default_value = 0.78
                multiply.inputs[2].default_value = targets[mat.name]
                mat.node_tree.links.new(source, multiply.inputs[1])
                for link in list(base.links):
                    mat.node_tree.links.remove(link)
                mat.node_tree.links.new(multiply.outputs[0], base)
            elif base:
                base.default_value = targets[mat.name]

# Imported high-detail licensed source models.
mosque_root, mosque_objects = import_group(
    "mosque_two_minarets.glb", "Emerald Ottoman mosque", 0.035, (0.0, 0.0, 0.0)
)
tint_mosque(mosque_objects)

tree_root, tree_objects = import_group(
    "broadleaf_canopy_tree.glb", "Blessed-tree-inspired broad canopy", 0.070, (-1.55, 0.15, 0.0)
)
cat_root, cat_objects = import_group(
    "sleeping_cat.glb", "Sleeping kitten", 1.18, (-1.22, -0.75, 0.015)
)
lantern_left_root, lantern_left_objects = import_group(
    "brass_hurricane_lantern.glb", "Left brass lantern", 2.65, (-0.98, -2.25, 0.0)
)
lantern_right_root, lantern_right_objects = import_group(
    "brass_hurricane_lantern.glb", "Right brass lantern", 2.65, (0.98, -2.25, 0.0)
)

# Warm gold, emerald textile and night-ground materials.
gold = pbr_material("Ramadan • brushed antique gold", (0.72, 0.39, 0.095), 0.82, 0.22)
gold_light = pbr_material("Ramadan • engraved gold", (0.94, 0.68, 0.25), 0.72, 0.2)
emerald = pbr_material("Ramadan • deep emerald silk", (0.012, 0.18, 0.105), 0.08, 0.48)
night_ground = pbr_material("Ramadan • midnight garden stone", (0.012, 0.035, 0.029), 0.18, 0.34)
flame_mat = pbr_material("Ramadan • living amber flame", (1.0, 0.19, 0.018), 0.0, 0.25, (1.0, 0.13, 0.008))

# A softly reflective ground plane, large enough to catch the lantern shadows.
bpy.ops.mesh.primitive_plane_add(size=200, location=(0, 0, -0.018))
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
for bp, co in zip(spline.bezier_points, [(-1.82, -2.55, 2.60), (0, -2.55, 2.47), (1.82, -2.55, 2.60)]):
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
            verts.append((x + (u - 0.5) * width, -2.56 + wave, z))
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
    text_curve.size = 0.245
    text_curve.extrude = 0.004
    text_curve.bevel_depth = 0.0015
    text_obj = bpy.data.objects.new("Gold lettering • " + letter, text_curve)
    bpy.context.collection.objects.link(text_obj)
    text_obj.location = (x, -2.595, 2.47 - height * 0.52)
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

# Real flame meshes and warm point lights inside the two glass lanterns.
def create_flame(name, x, y):
    levels = [(0.00, 0.075), (0.12, 0.065), (0.28, 0.042), (0.42, 0.018), (0.53, 0.0)]
    segments = 20
    verts, faces = [], []
    for z, radius in levels:
        for i in range(segments):
            a = 2 * math.pi * i / segments
            shift = 0.018 * (z / 0.53) ** 2
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
    obj.location.z = 0.14
    obj.scale = (1, 1, 1)
    for frame, sx, sy, sz in [(1, 1, 1, 1), (12, 0.92, 0.92, 1.08), (24, 1.06, 1.04, 0.94), (36, 0.94, 0.96, 1.05), (48, 1, 1, 1)]:
        obj.scale = (sx, sy, sz)
        obj.keyframe_insert(data_path="scale", frame=frame)
    light_data = bpy.data.lights.new(name + " • warm glow", "POINT")
    light_data.energy = 18
    light_data.color = (1.0, 0.32, 0.075)
    light_data.shadow_soft_size = 0.18
    light = bpy.data.objects.new(name + " • warm glow", light_data)
    bpy.context.collection.objects.link(light)
    light.location = (x, y, 0.38)
    for frame, energy in [(1, 18), (12, 23), (24, 16), (36, 22), (48, 18)]:
        light_data.energy = energy
        light_data.keyframe_insert(data_path="energy", frame=frame)
    return obj

create_flame("Left lantern • living flame", -0.98, -2.25)
create_flame("Right lantern • living flame", 0.98, -2.25)

# A subtle breathing motion for the sleeping kitten.
for frame, factor in [(1, 1.0), (30, 1.018), (60, 1.0)]:
    cat_root.scale = (1.18 * factor, 1.18, 1.18 * factor)
    cat_root.keyframe_insert(data_path="scale", frame=frame)

# Keep the scene in a gentle looping animation range.
scene = bpy.context.scene
scene.frame_start = 1
scene.frame_end = 60
scene.render.fps = 30
scene.world.color = (0.006, 0.012, 0.018)
scene.render.engine = "BLENDER_EEVEE_NEXT"
scene.render.resolution_x = 1920
scene.render.resolution_y = 1080
scene.render.resolution_percentage = 100

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
