extends Node2D

@onready var player: Node2D = $Player
@onready var dragon: Node2D = $Dragon

func _physics_process(delta: float) -> void:
    if dragon.position.distance_to(player.position) < 20:
        print("Game Over")
        get_tree().paused = true
