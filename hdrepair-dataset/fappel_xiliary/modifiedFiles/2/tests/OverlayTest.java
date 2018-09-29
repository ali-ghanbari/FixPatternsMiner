package com.codeaffine.eclipse.swt.widget.scrollbar;

import static com.codeaffine.eclipse.swt.test.util.DisplayHelper.flushPendingEvents;
import static com.codeaffine.eclipse.swt.test.util.SWTEventHelper.trigger;
import static com.codeaffine.eclipse.swt.testhelper.ShellHelper.openShell;
import static com.codeaffine.eclipse.swt.testhelper.ShellHelper.waitForGtkRendering;
import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codeaffine.eclipse.swt.test.util.DisplayHelper;

public class OverlayTest {

  private static final int X_LOCATION = 10;
  private static final int Y_LOCATION = 20;
  private static final int X_OFFSET = 1;
  private static final int Y_OFFSET = 2;

  @Rule
  public final DisplayHelper displayHelper = new DisplayHelper();

  private Composite toOverlay;
  private Overlay overlay;
  private Shell parent;

  @Before
  public void setUp() {
    parent = createParentShell();
    toOverlay = createCompositeToOverlay( parent );
    overlay = new Overlay( toOverlay );
    openShell( parent );
  }

  @Test
  public void keepParentShellActivated() {
    Shell control = overlay.getControl();
    control.setActive();

    overlay.keepParentShellActivated();
    flushPendingEvents();

    assertThat( getActiveShell() ).isSameAs( parent );
  }

  @Test
  public void initialization() {
    waitForGtkRendering();
    Shell actual = overlay.getControl();

    assertThat( actual.getAlpha() ).isEqualTo( Overlay.ALPHA );
    assertThat( actual.getBackgroundMode() ).isEqualTo( SWT.INHERIT_DEFAULT );
    assertThat( actual.getStyle() & SWT.NO_TRIM ).isEqualTo( SWT.NO_TRIM );
    assertThat( actual.getParent() ).isSameAs( parent );
    assertThat( actual.getLocation() ).isEqualTo( expectedInitialLocation() );
  }

  @Test
  public void mouseDown() {
    trigger( SWT.MouseDown ).on( overlay.getControl() );

    assertThat( getActiveShell() ).isSameAs( parent );
  }

  @Test
  public void moveToOverlay() {
    Point point = new Point( 20, 30 );
    Point expected = Display.getCurrent().map( parent, null, point );

    toOverlay.setLocation( point );
    waitForGtkRendering();
    Point actual = overlay.getControl().getLocation();

    assertThat( actual ).isEqualTo( expected );
  }

  @Test
  public void moveParentShell() {
    Point current = Display.getCurrent().map( parent, null, toOverlay.getLocation() );
    Point expected = addOffset( current, X_OFFSET, Y_OFFSET );

    waitForGtkRendering();
    parent.setLocation( addOffset( parent.getLocation(), X_OFFSET, Y_OFFSET ) );
    waitForGtkRendering();
    Point actual = overlay.getControl().getLocation();

    assertThat( actual ).isEqualTo( expected );
  }

  @Test
  public void controlResized() {
    Point expected = new Point( 323, 214 );

    toOverlay.setSize( expected );
    Point actual = overlay.getControl().getSize();

    assertThat( actual ).isEqualTo( expected );
  }

  @Test
  public void dispose() {
    parent.dispose();

    assertThat( overlay.getControl().isDisposed() ).isTrue();
  }

  private Shell createParentShell() {
    Shell result = displayHelper.createShell( SWT.SHELL_TRIM );
    result.setBounds( 200, 200, 500, 500 );
    result.open();
    return result;
  }

  private static Composite createCompositeToOverlay( Shell parent  ) {
    Composite result = new Composite( parent, SWT.NONE );
    result.setBounds( X_LOCATION, Y_LOCATION, 400, 400 );
    return result;
  }

  private Shell getActiveShell() {
    return displayHelper.getDisplay().getActiveShell();
  }

  private static Point addOffset( Point point , int xOffset , int yOffset  ) {
    return new Point( point.x + xOffset, point.y + yOffset );
  }

  private Point expectedInitialLocation() {
    Rectangle clientArea = parent.getClientArea();
    Point parentRelated = new Point( clientArea.x + X_LOCATION, clientArea.y + Y_LOCATION );
    return displayHelper.getDisplay().map( parent, null, parentRelated );
  }
}