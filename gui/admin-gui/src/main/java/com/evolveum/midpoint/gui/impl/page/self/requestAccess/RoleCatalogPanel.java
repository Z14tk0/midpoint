/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RoleCatalogPanel extends BasePanel implements WizardPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_AS_LIST = "asList";
    private static final String ID_AS_TILE = "asTile";
    private static final String ID_MENU = "menu";
    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";

    public RoleCatalogPanel(String id) {
        super(id);

        initLayout();
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("RoleCatalogPanel.title");
    }

    private void initLayout() {
        DetailsMenuPanel menu = new DetailsMenuPanel(ID_MENU);
        add(menu);

        ListView<CatalogTile> tiles = new ListView<>(ID_TILES) {

            @Override
            protected void populateItem(ListItem<CatalogTile> item) {
                CatalogTilePanel tile = new CatalogTilePanel(ID_TILE, item.getModel());
                item.add(tile);
            }
        };
        add(tiles);
    }
}