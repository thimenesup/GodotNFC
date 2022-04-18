extends Node 

var _info: RichTextLabel = null

var _nfc: Object = null

func _notification(what: int) -> void:
	if what == NOTIFICATION_INSTANCED:
		_info = $Info

func _ready() -> void:
	if Engine.has_singleton("GodotNFC"):
		_nfc = Engine.get_singleton("GodotNFC")
	
	if not _nfc:
		_info.text += "Failed getting NFC singleton\n"
		return
	
	_nfc.connect("nfc_enabled", self, "_on_nfc_enabled")
	_nfc.connect("tag_readed", self, "_on_nfc_tag_readed")
	_nfc.connect("tag_written", self, "_on_nfc_tag_written")
	
	_nfc.enableNFC()

func _on_nfc_enabled(status: int) -> void:
	_info.text += "NFC Status:%s\n" % status

func _on_nfc_tag_readed(data: PoolByteArray) -> void:
	_info.text += "NFC Read Data:%s\n" % data.get_string_from_utf8()

func _on_nfc_tag_written() -> void:
	_info.text += "NFC Tag written\n"
	$Input.clear()

func _process(delta: float) -> void:
	if _nfc:
		_nfc.pollTags()
	
	$Sprite.rotate(delta)


func _on_Input_text_entered(new_text: String) -> void:
	if _nfc:
		_nfc.queueWrite(new_text.to_utf8())
		_info.text += "NFC Queued Write:%s\n" % new_text
