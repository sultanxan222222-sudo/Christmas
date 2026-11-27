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
        "Cut stone, warm limestone": ((0.018, 0.19, 0.115, 1.0), 0.22, 0.30),
        "Lead sheet domes and roofs": ((0.008, 0.105, 0.065, 1.0), 0.48, 0.24),
        "Stone trims and cornices, light": ((0.11, 0.34, 0.23, 1.0), 0.52, 0.22),
        "Dome interior, gilded mosaic (plain)": ((0.24, 0.36, 0.19, 1.0), 0.62, 0.24),
        "Window glass, dark": ((0.006, 0.022, 0.018, 1.0), 0.18, 0.16),
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
    "mosque_two_minarets.glb", "Emerald Ottoman mosque", 0.068, (0.0, 0.0, 0.0)
)
tint_mosque(mosque_objects)

# Two mature broad-canopy trees frame the courtyard and physically anchor the banner rope.
tree_root, tree_objects = import_group(
    "broadleaf_canopy_tree.glb", "Blessed-tree-inspired left broad canopy", 0.090, (-2.28, -0.48, 0.0)
)
tree_right_root, tree_right_objects = import_group(
    "broadleaf_canopy_tree.glb", "Right banner-anchor broad canopy", 0.090, (2.28, -0.48, 0.0)
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
    "sleeping_cat.glb", "Sleeping kitten • clearly visible hero animal", 1.85, (-1.62, -1.05, 0.02)
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

# A softly reflective, hero-sized courtyard plane; it must not dominate Filament auto-framing.
bpy.ops.mesh.primitive_plane_add(size=12.0, location=(0, 0, -0.018))
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
for bp, co in zip(spline.bezier_points, [(-2.28, -0.62, 2.42), (0, -0.62, 2.27), (2.28, -0.62, 2.42)]):
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
            verts.append((x + (u - 0.5) * width, -0.63 + wave, z))
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
    text_obj.location = (x, -0.665, 2.42 - height * 0.52)
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
    for z, radius, minor in [(0.155, 0.125, 0.009), (0.335, 0.125, 0.009),
                             (0.185, 0.105, 0.004), (0.315, 0.105, 0.004)]:
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
            location=(x, y, 0.25)
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
                                    depth=0.055, location=(cx, cy, 0.405))
    finial = bpy.context.object
    finial.name = "Lantern • pointed finial"
    finial.data.materials.append(gold_light)

decorate_lantern(-0.98, -2.25)
decorate_lantern(0.98, -2.25)

# Real flame meshes and warm point lights inside the two glass lanterns.
def create_flame(name, x, y):
    # The source lantern glass occupies roughly z=0.16..0.34 after scaling.
    # Keep the whole flame inside that glass envelope instead of above the roof.
    levels = [(0.00, 0.024), (0.035, 0.022), (0.075, 0.015), (0.115, 0.007), (0.15, 0.0)]
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
    obj.location.z = 0.175
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
    z = star_rng.uniform(-2.55, -1.85)
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=radius, location=(x, y, z))
    star_obj = bpy.context.object
    star_obj.name = "Distant 3D star %02d" % index
    star_obj.data.materials.append(star_mat)

# A subtle breathing motion for the sleeping kitten.
for frame, factor in [(1, 1.0), (30, 1.018), (60, 1.0)]:
    cat_root.scale = (1.85 * factor, 1.85, 1.85 * factor)
    cat_root.keyframe_insert(data_path="scale", frame=frame)

# Keep the scene in a gentle looping animation range.
scene = bpy.context.scene
scene.frame_start = 1
scene.frame_end = 60
scene.render.fps = 30
scene.world.color = (0.006, 0.012, 0.018)
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
camera.location = (0.0, -15.0, 6.2)
target = Vector((0.0, -0.05, 1.45))
camera.rotation_euler = (target - camera.location).to_track_quat("-Z", "Y").to_euler()
camera_data.lens = 40
scene.camera = camera
key_data = bpy.data.lights.new("Preview softbox key", "AREA")
key_data.energy = 1450
key_data.shape = "DISK"
key_data.size = 7.0
key_obj = bpy.data.objects.new("Preview softbox key", key_data)
scene.collection.objects.link(key_obj)
key_obj.location = (0.0, -7.0, 8.0)
key_obj.rotation_euler = (Vector((0.0, 0.0, 1.0)) - key_obj.location).to_track_quat("-Z", "Y").to_euler()
scene.render.resolution_x = 1920
scene.render.resolution_y = 1080
scene.render.resolution_percentage = 60
scene.render.image_settings.file_format = "PNG"
scene.render.filepath = os.path.join(ROOT, "ramadan-3d-preview.png")
bpy.ops.render.render(write_still=True)
print("Rendered 3D scene preview:", scene.render.filepath)

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
