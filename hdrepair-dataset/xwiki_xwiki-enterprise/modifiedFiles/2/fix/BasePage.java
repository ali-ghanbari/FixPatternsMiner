/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.ui.framework.elements;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.framework.elements.editor.ClassEditPage;
import org.xwiki.test.ui.framework.elements.editor.ObjectEditPage;
import org.xwiki.test.ui.framework.elements.editor.RightsEditPage;
import org.xwiki.test.ui.framework.elements.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.framework.elements.editor.WikiEditPage;

/**
 * Represents the common actions possible on all Pages.
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class BasePage extends BaseElement
{
    /**
     * Used for sending keyboard shortcuts to.
     */
    @FindBy(id = "xwikimaincontainer")
    private WebElement mainContainerDiv;

    public String getPageTitle()
    {
        return getDriver().getTitle();
    }

    // TODO I think this should be in the AbstractTest instead -cjdelisle
    public String getPageURL()
    {
        return getDriver().getCurrentUrl();
    }

    public String getMetaDataValue(String metaName)
    {
        return getDriver().findElement(By.xpath("//meta[@name='" + metaName + "']")).getAttribute("content");
    }

    /**
     * @return true if we are currently logged in, false otherwise
     */
    public boolean isAuthenticated()
    {
        // Note that we cannot test if the userLink field is accessible since we're using an AjaxElementLocatorFactory
        // and thus it would wait 15 seconds before considering it's not accessible.
        return !getDriver().findElements(By.id("tmUser")).isEmpty();
    }

    /**
     * Determine if the current page is a new document.
     * 
     * @return true if the document is new, false otherwise
     */
    public boolean isNewDocument()
    {
        JavascriptExecutor js = (JavascriptExecutor) getDriver();
        return (Boolean) js.executeScript("return XWiki.docisnew");
    }

    /**
     * Emulate mouse over on a top menu entry.
     * 
     * @param menuId Menu to emulate the mouse over on
     */
    protected void hoverOverMenu(String menuId)
    {
        // We need to hover over the Wiki menu so that the menu entry is visible before we can click on
        // it. The normal way to implement it is to do something like this:
        //
        // @FindBy(id = "tmWiki")
        // private WebElement spaceMenuDiv;
        // ...
        // ((RenderedWebElement) spaceMenuDiv).hover();
        //
        // However it seems that currently Native Events don't work in FF 3.5+ versions and it seems to be only working
        // on Windows. Thus for now we have to simulate the hover using JavaScript.
        //
        // In addition there's a second bug where a WebElement retrieved using a @FindBy annotation cannot be used
        // as a parameter to JavascriptExecutor.executeScript().
        // See http://code.google.com/p/selenium/issues/detail?id=256
        // Thus FTM we have to use getDriver().findElement().

        // For some unknown reason sometimes the menuId cannot be found so wait for it to be visible before finding it.
        waitUntilElementIsVisible(By.id(menuId));

        WebElement menuDiv = getDriver().findElement(By.id(menuId));
        executeScript("showsubmenu(arguments[0])", menuDiv);

        // We wait for the submenu to be visible before carrying on to ensure that after this method returns the
        // calling code can access submenu items.
        waitUntilElementIsVisible(By.xpath("//div[@id = '" + menuId + "']//span[contains(@class, 'submenu')]"));
    }

    /**
     * Run javascript and return the result.
     * 
     * @param script The source of the script to run.
     * @param arguments Arguments to pass to the script. Will be made available to the script through a variable called
     *            "arguments".
     * @return Response from the script, One of Boolean, Long, String, List or WebElement. Or null.
     * @see <a
     *      href="http://webdriver.googlecode.com/svn/javadoc/org/openqa/selenium/JavascriptExecutor.html#executeScript">JavascriptExecutor</a>
     */
    public Object executeScript(String script, Object... arguments)
    {
        if (!(getDriver() instanceof JavascriptExecutor)) {
            throw new RuntimeException("This test only works with a Javascript-enabled Selenium2 Driver");
        }
        return ((JavascriptExecutor) getDriver()).executeScript(script, arguments);
    }

    /**
     * Perform a click on a "content menu" top entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuTopEntry(String id)
    {
        getDriver().findElement(By.xpath("//div[@id='" + id + "']//strong")).click();
    }

    /**
     * Perform a click on a "content menu" sub-menu entry.
     * 
     * @param id The id of the entry to follow
     */
    protected void clickContentMenuEditSubMenuEntry(String id)
    {
        hoverOverMenu("tmEdit");
        getDriver().findElement(By.xpath("//a[@id='" + id + "']")).click();
    }

    /**
     * Performs a click on the "edit" entry of the content menu.
     */
    public void edit()
    {
        clickContentMenuTopEntry("tmEdit");
    }

    /**
     * Performs a click on the "edit wiki" entry of the content menu.
     */
    public WikiEditPage editWiki()
    {
        clickContentMenuEditSubMenuEntry("tmEditWiki");
        return new WikiEditPage();
    }

    /**
     * Performs a click on the "edit wysiwyg" entry of the content menu.
     */
    public WYSIWYGEditPage editWYSIWYG()
    {
        clickContentMenuEditSubMenuEntry("tmEditWysiwyg");
        return new WYSIWYGEditPage();
    }

    /**
     * Performs a click on the "edit inline" entry of the content menu.
     */
    public InlinePage editInline()
    {
        clickContentMenuEditSubMenuEntry("tmEditInline");
        return new InlinePage();
    }

    /**
     * Performs a click on the "edit acces rights" entry of the content menu.
     */
    public RightsEditPage editRights()
    {
        clickContentMenuEditSubMenuEntry("tmEditRights");
        return new RightsEditPage();
    }

    /**
     * Performs a click on the "edit objects" entry of the content menu.
     */
    public ObjectEditPage editObjects()
    {
        clickContentMenuEditSubMenuEntry("tmEditObject");
        return new ObjectEditPage();
    }

    /**
     * Performs a click on the "edit class" entry of the content menu.
     */
    public ClassEditPage editClass()
    {
        clickContentMenuEditSubMenuEntry("tmEditClass");
        return new ClassEditPage();
    }

    /**
     * @since 2.6RC1
     */
    public void sendKeys(CharSequence... keys)
    {
        this.mainContainerDiv.sendKeys(keys);
    }

    /**
     * Waits until the page has loaded. Normally we don't need to call this method since a click in Selenium2 is a
     * blocking call. However there are cases (such as when using a shortcut) when we asynchronously load a page.
     * 
     * @since 2.6RC1
     */
    public void waitUntilPageIsLoaded()
    {
        waitUntilElementIsVisible(By.id("footerglobal"));
    }
}
