extends CharacterBody2D

@export var speed := 200.0
@export var jump_force := -400.0
var velocity: Vector2 = Vector2.ZERO

func _physics_process(delta: float) -> void:
    velocity.x = 0
    if Input.is_action_pressed("ui_left"):
        velocity.x = -speed
    elif Input.is_action_pressed("ui_right"):
        velocity.x = speed

    if is_on_floor() and Input.is_action_just_pressed("ui_accept"):
        velocity.y = jump_force

    velocity.y += 1000 * delta
    velocity = move_and_slide(velocity, Vector2.UP)
