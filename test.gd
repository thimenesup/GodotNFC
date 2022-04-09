extends Node 

var _info: Label = null

var _nfc: Object = null

func _notification(what: int) -> void:
	if what == NOTIFICATION_INSTANCED:
		_info = $Info

func _ready() -> void:
	if Engine.has_singleton("GodotNFC"):
		_nfc = Engine.get_singleton("GodotNFC")
	
	if not _nfc:
		_info.text = "Failed getting NFC singleton"
		return
	
	_nfc.connect("nfc_enabled", self, "_on_nfc_enabled")
	_nfc.connect("tag_readed", self, "_on_nfc_tag_readed")
	
	_nfc.enableNFC()

func _on_nfc_enabled(status: int) -> void:
	_info.text = "NFC Status:%s" % status

func _on_nfc_tag_readed(data: PoolByteArray) -> void:
	_info.text = "NFC Data:%s" % data.get_string_from_utf8()

func _process(delta: float) -> void:
	if _nfc:
		_nfc.pollTags()
	
	$Sprite.rotate(delta)
