extends Node2D

var GodotAndroidPlugin
var isNfcSupported = false

onready var label = $CenterContainer/Label

func _ready():
	print("alpha: _ready()")
	if Engine.has_singleton("GodotAndroidPlugin"):
		print("alpha: plugin found")
		GodotAndroidPlugin = Engine.get_singleton("GodotAndroidPlugin")
		isNfcSupported = GodotAndroidPlugin.isNfcSupported()
		if isNfcEnabled:
			print("alpha: nfc enabled")
			GodotAndroidPlugin.init()
			GodotAndroidPlugin.connect("send_data_to_godot", self, "_Data_Received")
		else:
			print("alpha: nfc not enabled")
	else:
		print("alpha: plugin not found")

func _process(delta):
	if GodotAndroidPlugin and isNfcSupported:
		GodotAndroidPlugin.pollTags()

func _Data_Received(data:String) :
	print(data)
	label.text = data
