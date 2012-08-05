package net.minecraft.src;

import java.awt.Graphics;
import java.awt.image.*;

public class ImageBufferDownload implements ImageBuffer
{
    private int imageData[];
    private int imageWidth;
    private int imageHeight;

    public ImageBufferDownload()
    {
    }

    public BufferedImage parseUserSkin(BufferedImage par1BufferedImage)
    {
        if (par1BufferedImage == null)
        {
            return null;
        }

        imageWidth = 64;
        imageHeight = 32;

        BufferedImage checkImg = par1BufferedImage;
        while (imageWidth < checkImg.getWidth() || imageHeight < checkImg.getHeight()) {
        	imageWidth	*= 2;
        	imageHeight	*= 2;
        }
        
        BufferedImage bufferedimage = new BufferedImage(imageWidth, imageHeight, 2);
        Graphics g = bufferedimage.getGraphics();
        g.drawImage(par1BufferedImage, 0, 0, null);
        g.dispose();
        imageData = ((DataBufferInt)bufferedimage.getRaster().getDataBuffer()).getData();
        func_884_b(0, 0, imageWidth / 2, imageHeight / 2);
        func_885_a(imageWidth / 2, 0, imageWidth, imageHeight);
        func_884_b(0, imageHeight / 2, imageWidth, imageHeight);
        boolean flag = false;

        for (int i = imageWidth/2; i < imageWidth; i++)
        {
            for (int k = 0; k < imageHeight/2; k++)
            {
                int i1 = imageData[i + k * imageWidth];

                if ((i1 >> 24 & 0xff) < 128)
                {
                    flag = true;
                }
            }
        }

        if (!flag)
        {
            for (int j = imageWidth/2; j < imageWidth; j++)
            {
                for (int l = 0; l < imageHeight/2; l++)
                {
                    int j1 = imageData[j + l * imageWidth];
                    boolean flag1;

                    if ((j1 >> 24 & 0xff) < 128)
                    {
                        flag1 = true;
                    }
                }
            }
        }

        return bufferedimage;
    }

    private void func_885_a(int par1, int par2, int par3, int par4)
    {
        if (func_886_c(par1, par2, par3, par4))
        {
            return;
        }

        for (int i = par1; i < par3; i++)
        {
            for (int j = par2; j < par4; j++)
            {
                imageData[i + j * imageWidth] &= 0xffffff;
            }
        }
    }

    private void func_884_b(int par1, int par2, int par3, int par4)
    {
        for (int i = par1; i < par3; i++)
        {
            for (int j = par2; j < par4; j++)
            {
                imageData[i + j * imageWidth] |= 0xff000000;
            }
        }
    }

    private boolean func_886_c(int par1, int par2, int par3, int par4)
    {
        for (int i = par1; i < par3; i++)
        {
            for (int j = par2; j < par4; j++)
            {
                int k = imageData[i + j * imageWidth];

                if ((k >> 24 & 0xff) < 128)
                {
                    return true;
                }
            }
        }

        return false;
    }
}
