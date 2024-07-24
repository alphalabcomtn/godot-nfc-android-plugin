extends Node2D

var GodotAndroidPlugin

onready var label = $CenterContainer/Label

func _ready():
	print("alpha: _ready()")
	GodotAndroidPlugin = Engine.get_singleton("GodotAndroidPlugin")
	GodotAndroidPlugin.init()
	GodotAndroidPlugin.connect("send_data_to_godot", self, "_Data_Received")

func _process(delta):
	if GodotAndroidPlugin:
		GodotAndroidPlugin.pollTags()

func _Data_Received(data:String) :
	print(data)
	label.text = data
