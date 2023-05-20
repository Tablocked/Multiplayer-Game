package tablock.userInterface;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import tablock.core.Input;
import tablock.network.Client;

import java.util.ArrayList;
import java.util.List;

public class InputIndicator
{
    private double totalWidth = 0;
    private final List<String> texts = new ArrayList<>();
    private final List<Image> images = new ArrayList<>();

    public void add(String text, Image image)
    {
        if(text != null)
        {
            double textWidth = Client.computeTextShape(text, Font.font("Arial", 40)).getWidth();
            double imageWidth = image.getWidth();

            totalWidth += textWidth + imageWidth;

            texts.add(text);
            images.add(image);
        }
    }

    public void add(String text, Input input)
    {
        add(text, input.getImage());
    }

    public void render(GraphicsContext gc)
    {
        double xOffset = (1920 - totalWidth) / (texts.size() + 1);
        double xPosition = xOffset;

        gc.setFill(Color.rgb(50, 50, 50));
        gc.fillRoundRect(xPosition - 5, 995, totalWidth + (xOffset * (texts.size() - 1)) + 20, 75, 50, 50);
        gc.setFont(Font.font("Arial", 40));
        gc.setFill(Color.WHITE);

        for(int i = 0; i < texts.size(); i++)
        {
            String text = texts.get(i);
            Image image = images.get(i);
            double textWidth = Client.computeTextShape(text, gc).getWidth();

            gc.drawImage(image, xPosition, 1000);
            gc.fillText(text, xPosition + image.getWidth(), 1046);

            xPosition += textWidth + image.getWidth() + xOffset;
        }

        totalWidth = 0;

        texts.clear();
        images.clear();
    }
}