package sh.komet.gui.control.datetimepicker;


import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXRippler.RipplerMask;
import com.jfoenix.transitions.JFXAnimationTimer;
import com.jfoenix.transitions.JFXKeyFrame;
import com.jfoenix.transitions.JFXKeyValue;
import javafx.animation.Interpolator;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.skin.RadioButtonSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * <h1>Material Design Radio Button Skin</h1>
 *
 * @author Shadi Shaheen
 * @version 2.0
 * @since 2016-09-29
 */
public class JfxRadioButtonSkin extends RadioButtonSkin {

    private final JFXRippler rippler;
    private double padding = 12;

    private Circle radio, dot;
    private final StackPane container;

    private JFXAnimationTimer timer;

    public JfxRadioButtonSkin(JfxRadioButton control) {
        super(control);

        final double radioRadius = 8;
        radio = new Circle(radioRadius);
        radio.getStyleClass().setAll("radio");
        radio.setStrokeWidth(2);
        radio.setFill(Color.TRANSPARENT);
        radio.setSmooth(true);

        dot = new Circle(radioRadius);
        dot.getStyleClass().setAll("dot");
        dot.fillProperty().bind(control.selectedColorProperty());
        dot.setScaleX(0);
        dot.setScaleY(0);
        dot.setSmooth(true);

        container = new StackPane(radio, dot);
        container.getStyleClass().add("radio-container");

        rippler = new JFXRippler(container, RipplerMask.CIRCLE) {
            @Override
            protected double computeRippleRadius() {
                double width = ripplerPane.getWidth();
                double width2 = width * width;
                return Math.min(Math.sqrt(width2 + width2), RIPPLE_MAX_RADIUS) * 1.1 + 5;
            }

            @Override
            protected void setOverLayBounds(Rectangle overlay) {
                overlay.setWidth(ripplerPane.getWidth());
                overlay.setHeight(ripplerPane.getHeight());
            }

            protected void initControlListeners() {
                // if the control got resized the overlay rect must be rest
                control.layoutBoundsProperty().addListener(observable -> resetRippler());
                if (getChildren().contains(control)) {
                    control.boundsInParentProperty().addListener(observable -> resetRippler());
                }
                control.addEventHandler(MouseEvent.MOUSE_PRESSED,
                        (event) -> createRipple(event.getX() + padding, event.getY() + padding));
                // create fade out transition for the ripple
                control.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> releaseRipple());
            }

            @Override
            protected Node getMask() {
                double radius = ripplerPane.getWidth() / 2;
                return new Circle(radius, radius, radius);
            }

            @Override
            protected void positionControl(Node control) {

            }
        };

        updateChildren();

        // show focused state
        control.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (!control.disableVisualFocusProperty().get()) {
                if (newVal) {
                    if (!getSkinnable().isPressed()) {
                        rippler.setOverlayVisible(true);
                    }
                } else {
                    rippler.setOverlayVisible(false);
                }
            }
        });

        control.pressedProperty().addListener((o, oldVal, newVal) -> rippler.setOverlayVisible(false));

        timer = new JFXAnimationTimer(
                new JFXKeyFrame(Duration.millis(200),
                        JFXKeyValue.builder()
                                .setTarget(dot.scaleXProperty())
                                .setEndValueSupplier(() -> getSkinnable().isSelected() ? 0.55 : 0)
                                .setInterpolator(Interpolator.EASE_BOTH)
                                .build(),
                        JFXKeyValue.builder()
                                .setTarget(dot.scaleYProperty())
                                .setEndValueSupplier(() -> getSkinnable().isSelected() ? 0.55 : 0)
                                .setInterpolator(Interpolator.EASE_BOTH)
                                .build(),
                        JFXKeyValue.builder()
                                .setTarget(radio.strokeProperty())
                                .setEndValueSupplier(() -> getSkinnable().isSelected() ? ((JfxRadioButton) getSkinnable()).getSelectedColor() : ((JfxRadioButton) getSkinnable()).getUnSelectedColor())
                                .setInterpolator(Interpolator.EASE_BOTH)
                                .build()
                ));


        registerChangeListener(control.selectedColorProperty(), obs -> updateColors());
        registerChangeListener(control.unSelectedColorProperty(), obs -> updateColors());
        registerChangeListener(control.selectedProperty(), obs -> {
            boolean isSelected = getSkinnable().isSelected();
            Color unSelectedColor = ((JfxRadioButton) getSkinnable()).getUnSelectedColor();
            Color selectedColor = ((JfxRadioButton) getSkinnable()).getSelectedColor();
            rippler.setRipplerFill(isSelected ? selectedColor : unSelectedColor);
            if (((JfxRadioButton) getSkinnable()).isDisableAnimation()) {
                // apply end values
                timer.applyEndValues();
            } else {
                // play selection animation
                timer.reverseAndContinue();
            }
        });

        updateColors();
        timer.applyEndValues();
    }

    @Override
    protected void updateChildren() {
        super.updateChildren();
        if (radio != null) {
            removeRadio();
            getChildren().addAll(container, rippler);
        }
    }

    @Override
    protected void layoutChildren(final double x, final double y, final double w, final double h) {
        final RadioButton radioButton = getSkinnable();
        final double contWidth = snapSizeX(container.prefWidth(-1));
        final double contHeight = snapSizeY(container.prefHeight(-1));
        final double computeWidth = Math.max(radioButton.prefWidth(-1), radioButton.minWidth(-1));
        final double width = snapSizeX(contWidth);
        final double height = snapSizeY(contHeight);

        final double labelWidth = Math.min(computeWidth - contWidth, w - width);
        final double labelHeight = Math.min(radioButton.prefHeight(labelWidth), h);
        final double maxHeight = Math.max(contHeight, labelHeight);
        final double xOffset = computeXOffset(w, labelWidth + contWidth, radioButton.getAlignment().getHpos()) + x;
        final double yOffset = computeYOffset(h, maxHeight, radioButton.getAlignment().getVpos()) + y;

        container.resize(width, height);
        layoutLabelInArea(xOffset + contWidth + padding / 3, yOffset, labelWidth, maxHeight, radioButton.getAlignment());
        positionInArea(container,
                xOffset,
                yOffset,
                contWidth,
                maxHeight,
                0,
                radioButton.getAlignment().getHpos(),
                radioButton.getAlignment().getVpos());
        final double ripplerWidth = width + 2 * padding;
        final double ripplerHeight = height + 2 * padding;
        rippler.resizeRelocate((width / 2 + xOffset) - ripplerWidth / 2,
                (height / 2 + xOffset) - ripplerHeight / 2,
                ripplerWidth, ripplerHeight);
    }

    private void removeRadio() {
        // TODO: replace with removeIf
        for (int i = 0; i < getChildren().size(); i++) {
            if (getChildren().get(i).getStyleClass().contains("radio")) {
                getChildren().remove(i);
                break;
            }
        }
    }

    private void updateColors() {
        boolean isSelected = getSkinnable().isSelected();
        Color unSelectedColor = ((JfxRadioButton) getSkinnable()).getUnSelectedColor();
        Color selectedColor = ((JfxRadioButton) getSkinnable()).getSelectedColor();
        rippler.setRipplerFill(isSelected ? selectedColor : unSelectedColor);
        radio.setStroke(isSelected ? selectedColor : unSelectedColor);
    }

    @Override
    protected double computeMinWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computeMinWidth(height,
                topInset,
                rightInset,
                bottomInset,
                leftInset) + snapSizeX(radio.minWidth(-1)) + padding / 3;
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset, double bottomInset, double leftInset) {
        return super.computePrefWidth(height,
                topInset,
                rightInset,
                bottomInset,
                leftInset) + snapSizeX(radio.prefWidth(-1)) + padding / 3;
    }

    private static double computeXOffset(double width, double contentWidth, HPos hpos) {
        switch (hpos) {
            case LEFT:
                return 0;
            case CENTER:
                return (width - contentWidth) / 2;
            case RIGHT:
                return width - contentWidth;
        }
        return 0;
    }

    private static double computeYOffset(double height, double contentHeight, VPos vpos) {
        switch (vpos) {
            case TOP:
                return 0;
            case CENTER:
                return (height - contentHeight) / 2;
            case BOTTOM:
                return height - contentHeight;
            default:
                return 0;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        timer.dispose();
        timer = null;
    }
}