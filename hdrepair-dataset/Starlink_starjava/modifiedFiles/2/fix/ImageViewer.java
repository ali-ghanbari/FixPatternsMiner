package uk.ac.starlink.treeview;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.io.IOException;
import javax.swing.*;
import javax.media.jai.PlanarImage;
import uk.ac.starlink.ast.FrameSet;
import uk.ac.starlink.ast.Plot;
import uk.ac.starlink.array.NDArray;
import jsky.image.ImageProcessor;
import jsky.image.gui.ImageDisplay;

/**
 * Displays the pixels of a 2-d array, optionally with an AST grid plotted
 * over the top.
 * There is currently no provision for scaling, modifying the colour map,
 * or anything else flashy.
 */
class ImageViewer extends JPanel {

    /**
     * Construct an image view from an NDArray.
     *
     * @param  nda   a 2-dimensional readable NDArray with random access.
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     * @throws  IOException  if there is an error in data access
     */
    public ImageViewer( NDArray nda, FrameSet wcs ) throws IOException {
        this( new NDArrayImage( nda ), nda.getShape().getOrigin(),
              nda.getBadHandler().getBadValue(), wcs );
    }


    /**
     * Construct an image view given a RenderedImage.  It may have a 
     * non-zero origin and which optionally displays coordinate grids. 
     *
     * @param  im      a RenderedImage on which to base the display
     * @param  origin  a 2-element array giving the pixel coords of the 
     *                 data origin
     * @param  badval  magic bad value as a Number
     * @param  wcs     an AST frameset containing coordinate information.
     *                 May be null
     */
    private ImageViewer( RenderedImage im, long[] origin, Number badValue, 
                         FrameSet wcs ) {

        /* Get the bad value as a double. */
        final double badval = ( badValue == null ) ? Double.NaN 
                                                   : badValue.doubleValue();

        /* Turn it into a PlanarImage and do more setup. */
        PlanarImage pim = PlanarImage.wrapRenderedImage( im );
        Dimension picsize = new Dimension( pim.getWidth(), pim.getHeight() );
        final ImageDisplay disp = new ImageDisplay();
        disp.setPrescaled( true );
        disp.setImmediateMode( true );
        disp.setPreferredSize( picsize );

        /* Arrange coordinate plotting if necessary. */
        if ( wcs != null ) {

            /* Create a Plot with enough space round it to do axis labelling. */
            int lgap = 70;
            int rgap = 20;
            int tgap = 40;
            int bgap = 50;
            Dimension holdersize = 
                new Dimension( pim.getWidth() + lgap + rgap,
                               pim.getHeight() + tgap + bgap );
            Rectangle outRect = new Rectangle( holdersize );
            Rectangle inRect = new Rectangle( lgap, tgap, 
                                              pim.getWidth(), pim.getHeight() );
            double[] basebox = new double[ 4 ];
            basebox[ 0 ] = origin[ 0 ] + 0.5;
            basebox[ 1 ] = origin[ 1 ] + 0.5;
            basebox[ 2 ] = origin[ 0 ] + 0.5 + pim.getWidth();
            basebox[ 3 ] = origin[ 1 ] + 0.5 + pim.getHeight();
            final Plot plot = new Plot( wcs, outRect, basebox, 
                                        lgap, rgap, bgap, tgap );

            /* Configure the Plot. */
            plot.setGrid( true );
            plot.setColour( "grid", 0x8000ff00 );
            plot.setColour( "ticks", Color.GREEN.getRGB() );
            plot.setColour( "textlab", Color.BLACK.getRGB() );
            plot.grid();

            /* Make a panel which will cope with redrawing it as required. */
            JPanel plotpan = new JPanel() {
                protected void paintComponent( Graphics g ) {
                    super.paintComponent( g );
                    plot.paint( g );
                }
            };
            plotpan.setOpaque( false );

            /* Put both the image display and the plot panel into a new 
             * conainer which has no LayoutManager, doing placement manually
             * so they line up properly (tried to do this with an OverlayLayout
             * but couldn't get it to work). */
            JPanel holder = new JPanel();
            holder.setLayout( null );
            holder.setPreferredSize( holdersize );
            holder.add( plotpan );
            holder.add( disp );
            plotpan.setBounds( outRect );
            disp.setBounds( inRect );

            /* Add the whole lot to this panel. */
            this.add( holder );
        }

        /* No coordinates - just stick the PlanarImage into this panel. */
        else {
            this.add( disp );
        }

        /* Configure the image display panel. */
        // This part should be done out of the Event Dispatcher thread
        // really, but whenever I try to put it in a SwingWorker it ends
        // up failing to set the data properly (get a blank screen).
        int tx = Math.min( pim.getTileWidth(), pim.getWidth() );
        int ty = Math.min( pim.getTileHeight(), pim.getHeight() );
        Rectangle2D.Double sample = new Rectangle2D.Double( 0.0, 0.0, tx, ty );
        final ImageProcessor ip = new ImageProcessor( pim, sample ) {
            {  
                setBlank( badval );
            }
        };
        ip.setReverseY( true );
        boolean done = false;
        for ( int ix = 0; ix < pim.getWidth() / tx && ! done; ix++ ) {
            for ( int iy = 0; iy < pim.getHeight() / ty && ! done; iy++ ) {
                Rectangle2D.Double samp = 
                    new Rectangle2D.Double( (double) ix * tx, (double) iy * ty,
                                            tx, ty );
                ip.autoSetCutLevels( 98.0, samp );
                if ( ip.getLowCut() < ip.getHighCut() ) {
                    done = true;
                }
            }
        }
        ip.update();
        disp.setImageProcessor( ip );
    }

} 
