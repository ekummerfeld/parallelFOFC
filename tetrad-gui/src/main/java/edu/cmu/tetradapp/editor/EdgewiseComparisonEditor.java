///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////

package edu.cmu.tetradapp.editor;

import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetradapp.model.EdgewiseComparisonModel;
import edu.cmu.tetradapp.model.GraphWrapper;

import javax.swing.*;
import java.awt.*;

/**
 * Provides a little display/editor for notes in the session workbench. This
 * may be elaborated in the future to allow marked up text.
 *
 * @author Joseph Ramsey
 */
public class EdgewiseComparisonEditor extends JPanel {

    /**
     * The model for the note.
     */
    private final EdgewiseComparisonModel comparison;


    /**
     * Constructs the editor given the model
     */
    public EdgewiseComparisonEditor(EdgewiseComparisonModel comparison) {
        this.comparison = comparison;
        setup();
    }

    //============================ Private Methods =========================//

    private void setup() {
        java.util.List<Graph> referenceGraphs = comparison.getReferenceGraphs();
        JTabbedPane pane = new JTabbedPane(JTabbedPane.LEFT);

        for (int i = 0; i < referenceGraphs.size(); i++) {
            JTabbedPane pane2 = new JTabbedPane(JTabbedPane.TOP);
            String compareString = comparison.getComparisonString(i);

            JPanel panel = new JPanel();

            Font font = new Font("Monospaced", Font.PLAIN, 14);
            final JTextArea textPane = new JTextArea();
            textPane.setText(compareString);

            textPane.setFont(font);

            JScrollPane scroll = new JScrollPane(textPane);
            scroll.setPreferredSize(new Dimension(400, 400));

            panel.add(Box.createVerticalStrut(10));

            Box box = Box.createHorizontalBox();
            panel.add(box);
            panel.add(Box.createVerticalStrut(10));

            Box box1 = Box.createHorizontalBox();
            box1.add(new JLabel("Graph Comparison: "));
            box1.add(Box.createHorizontalGlue());

            add(box1);
            setLayout(new BorderLayout());

            pane2.add("Comparison", scroll);

            pane2.add("Target", new GraphEditor(new GraphWrapper(comparison.getTargetGraphs().get(i))).getWorkbench());
            pane2.add("Reference", new GraphEditor(new GraphWrapper(comparison.getReferenceGraphs().get(i))).getWorkbench());

            pane.add("" + (i + 1), pane2);


        }

        add(pane);
    }

}



