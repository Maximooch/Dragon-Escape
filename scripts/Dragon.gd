extends CharacterBody2D

@export var speed := 150.0
var velocity: Vector2 = Vector2.ZERO
@onready var player: Node2D = get_parent().get_node("Player")

func _physics_process(delta: float) -> void:
    if player.position.x > position.x:
        velocity.x = speed
    else:
        velocity.x = -speed
    velocity = move_and_slide(velocity, Vector2.UP)
