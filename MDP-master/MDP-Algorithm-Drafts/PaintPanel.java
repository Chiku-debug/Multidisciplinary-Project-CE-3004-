import java.awt.Graphics;
import java.awt.event.ActionListener;

import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.awt.event.ActionEvent;

import javax.swing.*;

public class PaintPanel extends JPanel {
    /**
     * PaintPanel is an extension of JPanel (aka its a Jpanel too)
     * 
     *Calls the override draw method of PaintDisplay to draw all the shapes that were in the list.
     */
    private static final long serialVersionUID = 1L;
    private PaintDisplay paintList;

    public PaintPanel(PaintDisplay paintList) {
        this.paintList = paintList;
    }

    public void setPaintList(PaintDisplay paintList){
        this.paintList = paintList;
    }

    
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintList.draw(g);
    }
}