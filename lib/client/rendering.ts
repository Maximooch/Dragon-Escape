type SceneFog<Color> = {
  readonly fog: {
    type: string;
    color: Color;
    density: number;
  };
};

export function configureSceneFog<Color>(
  scene: SceneFog<Color>,
  type: string,
  color: Color,
  density: number,
) {
  scene.fog.type = type;
  scene.fog.color = color;
  scene.fog.density = density;
}
