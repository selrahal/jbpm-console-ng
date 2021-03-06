/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.console.ng.es.client.editors.requestlist;

import java.util.*;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jbpm.console.ng.es.client.editors.jobdetails.JobDetailsPopup;
import org.jbpm.console.ng.es.client.editors.quicknewjob.QuickNewJobPopup;
import org.jbpm.console.ng.es.client.editors.servicesettings.JobServiceSettingsPopup;
import org.jbpm.console.ng.es.client.i18n.Constants;
import org.jbpm.console.ng.es.model.RequestSummary;
import org.jbpm.console.ng.es.model.events.RequestChangedEvent;
import org.jbpm.console.ng.gc.client.experimental.grid.base.ExtendedPagedTable;
import org.jbpm.console.ng.gc.client.list.base.AbstractListView;

import org.uberfire.ext.services.shared.preferences.GridGlobalPreferences;
import org.uberfire.ext.widgets.common.client.tables.DataGridFilter;
import org.uberfire.ext.widgets.common.client.tables.popup.NewFilterPopup;

import org.uberfire.mvp.Command;
import org.uberfire.workbench.events.NotificationEvent;

import com.github.gwtbootstrap.client.ui.NavLink;
import com.github.gwtbootstrap.client.ui.SplitDropdownButton;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ActionCell.Delegate;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;

@Dependent
public class RequestListViewImpl extends AbstractListView<RequestSummary,RequestListPresenter> implements RequestListPresenter.RequestListView{

    private Constants constants = GWT.create( Constants.class );

    @Inject
    private Event<NotificationEvent> notification;

    private List<RequestSummary> selectedRequestSummary = new ArrayList<RequestSummary>();

    @Inject
    private JobDetailsPopup jobDetailsPopup;

    @Inject
    private QuickNewJobPopup quickNewJobPopup;

    @Inject
    private NewFilterPopup newFilterPopup;



    @Inject
    private JobServiceSettingsPopup jobServiceSettingsPopup;

    @Override
    public void init(final RequestListPresenter presenter ) {
        final List<String> bannedColumns = new ArrayList<String>();
        bannedColumns.add(constants.Id());
        bannedColumns.add(constants.Type());
        final List<String> initColumns = new ArrayList<String>();
        initColumns.add(constants.Id());
        initColumns.add(constants.Type());
        initColumns.add(constants.Actions());

        super.init(presenter, new GridGlobalPreferences("RequestListGrid", initColumns, bannedColumns));
        initSelectionModel( listGrid );
    }

    public void requestCreated( @Observes RequestChangedEvent event ) {
        presenter.refreshRequests(null);
    }


    @Override
    public void initColumns(  ) {

        initJobIdColumn(listGrid);
        initJobTypeColumn(listGrid);
        initStatusColumn(listGrid);
        initDueDateColumn(listGrid);
        actionsColumn = initActionsColumn();
        listGrid.addColumn( actionsColumn, constants.Actions() );
    }

    @Override
    public void initFilters( ) {

        final ExtendedPagedTable extendedPagedTable =listGrid;
        final String dataProviderKey="base";

        extendedPagedTable.setShowFilterSelector( true );
        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "showAll", Constants.INSTANCE.All(), new Command() {
            @Override
            public void execute() {
                presenter.refreshRequests( dataProviderKey,null );
            }
        } ) );

        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "queued", Constants.INSTANCE.Queued(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "QUEUED" );
                presenter.refreshRequests( dataProviderKey, statuses );
            }
        } ) );

        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "running", Constants.INSTANCE.Running(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "RUNNING" );
                presenter.refreshRequests(dataProviderKey, statuses );
            }
        } ) );

        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "retrying", Constants.INSTANCE.Retrying(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "RETRYING" );
                presenter.refreshRequests( dataProviderKey,statuses );
            }
        } ) );

        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "error", Constants.INSTANCE.Error(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "ERROR" );
                presenter.refreshRequests( dataProviderKey,statuses );
            }
        } ) );

        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "showCompleted", Constants.INSTANCE.Completed(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "DONE" );
                presenter.refreshRequests( dataProviderKey,statuses );
            }
        } ) );
        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "showCancelled", Constants.INSTANCE.Cancelled(), new Command() {
            @Override
            public void execute() {
                List<String> statuses = new ArrayList<String>();
                statuses.add( "CANCELLED" );
                presenter.refreshRequests( dataProviderKey, statuses );
            }
        } ) );

        HashMap storedCustomFilters = extendedPagedTable.getStoredCustomFilters();
        if(storedCustomFilters!=null) {
            Set customFilterKeys = storedCustomFilters.keySet();
            Iterator it = customFilterKeys.iterator();
            String customFilterName;

            while ( it.hasNext() ) {
                customFilterName = ( String ) it.next();

                final HashMap filterValues = ( HashMap ) storedCustomFilters.get( customFilterName );

                extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( customFilterName, customFilterName,
                        new Command() {
                            @Override
                            public void execute() {
                                List<String> statuses = ( List ) filterValues.get( "states" );
                                presenter.refreshRequests( dataProviderKey, statuses );
                            }
                        } ) );


            }
        }
        final Command refreshFilterDropDownCommand = new Command() {
            @Override
            public void execute() {
                extendedPagedTable.clearFilters();
                initFilters( );
            }
        };
        extendedPagedTable.addFilter( new DataGridFilter<RequestSummary>( "addFilter", "-- " + Constants.INSTANCE.FilterManagement() + " --",
                new Command() {
                    @Override
                    public void execute() {
                        Command addFilter = new Command() {
                            @Override
                            public void execute() {
                                final String newFilterName = ( String ) newFilterPopup.getFormValues().get( NewFilterPopup.FILTER_NAME_PARAM );
                                extendedPagedTable.storeNewCustomFilter( newFilterName, newFilterPopup.getFormValues() );
                                extendedPagedTable.clearFilters();
                                initFilters(  );
                            }
                        };
                        createFilterForm();
                        newFilterPopup.show( addFilter, refreshFilterDropDownCommand, extendedPagedTable.getGridPreferencesStore() );
                    }
                }  ));
        extendedPagedTable.refreshFilterDropdown();

    }
    private void createFilterForm(){
        HashMap<String,String> statesListBoxInfo = new HashMap<String, String>(  );

        statesListBoxInfo.put( String.valueOf( "QUEUED" ), Constants.INSTANCE.Queued() );
        statesListBoxInfo.put( String.valueOf( "RUNNING" ), Constants.INSTANCE.Running() );
        statesListBoxInfo.put( String.valueOf( "RETRYING" ), Constants.INSTANCE.Retrying() );
        statesListBoxInfo.put( String.valueOf( "ERROR" ), Constants.INSTANCE.Error() );
        statesListBoxInfo.put( String.valueOf( "DONE" ), Constants.INSTANCE.Completed() );
        statesListBoxInfo.put( String.valueOf( "CANCELLED" ), Constants.INSTANCE.Cancelled() );

        newFilterPopup.init();
        newFilterPopup.addListBoxToFilter( Constants.INSTANCE.Status(), "states",true,statesListBoxInfo );

    }


    private void initSelectionModel( final ExtendedPagedTable extendedPagedTable){

        this.initActionsDropDown(extendedPagedTable);
        extendedPagedTable.setEmptyTableCaption( constants.No_Jobs_Found() );
        selectionModel = new NoSelectionModel<RequestSummary>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {

                boolean close = false;
                if (selectedRow == -1) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();

                } else if (extendedPagedTable.getKeyboardSelectedRow() != selectedRow) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();
                } else {
                    close = true;
                }
            }
        });
        initNoActionColumnManager(extendedPagedTable);

        extendedPagedTable.setSelectionModel( selectionModel, noActionColumnManager );
        extendedPagedTable.setRowStyles( selectedStyles );
    }

    private void initNoActionColumnManager( final ExtendedPagedTable extendedPagedTable){
        noActionColumnManager = DefaultSelectionEventManager
                .createCustomManager(new DefaultSelectionEventManager.EventTranslator<RequestSummary>() {

                    @Override
                    public boolean clearCurrentSelection(CellPreviewEvent<RequestSummary> event) {
                        return false;
                    }

                    @Override
                    public DefaultSelectionEventManager.SelectAction translateSelectionEvent(CellPreviewEvent<RequestSummary> event) {
                        NativeEvent nativeEvent = event.getNativeEvent();
                        if (BrowserEvents.CLICK.equals(nativeEvent.getType())) {
                            // Ignore if the event didn't occur in the correct column.
                            if (extendedPagedTable.getColumnIndex(actionsColumn) == event.getColumn()) {
                                return DefaultSelectionEventManager.SelectAction.IGNORE;
                            }
                            //Extension for checkboxes
                            Element target = nativeEvent.getEventTarget().cast();
                            if ("input".equals(target.getTagName().toLowerCase())) {
                                final InputElement input = target.cast();
                                if ("checkbox".equals(input.getType().toLowerCase())) {
                                    // Synchronize the checkbox with the current selection state.
                                    if (!selectedRequestSummary.contains(event.getValue())) {
                                        selectedRequestSummary.add(event.getValue());
                                        input.setChecked(true);
                                    } else {
                                        selectedRequestSummary.remove(event.getValue());
                                        input.setChecked(false);
                                    }
                                    return DefaultSelectionEventManager.SelectAction.IGNORE;
                                }
                            }
                        }

                        return DefaultSelectionEventManager.SelectAction.DEFAULT;
                    }

                });

    }

    private void initActionsDropDown(ExtendedPagedTable extendedPagedTable) {
        SplitDropdownButton actions = new SplitDropdownButton();
        actions.setText( constants.Actions() );
        NavLink newJobNavLink = new NavLink(constants.New_Job());
        newJobNavLink.setIcon(IconType.PLUS_SIGN);
        newJobNavLink.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                //placeManager.goTo( new DefaultPlaceRequest( "Quick New Job" ) );
                quickNewJobPopup.show();
            }
        } );

        NavLink settingsNavLink = new NavLink(constants.Settings());
        settingsNavLink.setIcon( IconType.COG );
        settingsNavLink.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                jobServiceSettingsPopup.show();
                //placeManager.goTo( new DefaultPlaceRequest( "Job Service Settings" ) );
            }
        } );

        actions.add( newJobNavLink );
        actions.add( settingsNavLink );
        extendedPagedTable.getLeftToolbar().add( actions );
    }



    private void initJobIdColumn(ExtendedPagedTable extendedPagedTable){
        // Id
        Column<RequestSummary, Number> taskIdColumn = new Column<RequestSummary, Number>( new NumberCell() ) {
            @Override
            public Number getValue( RequestSummary object ) {
                return object.getJobId();
            }
        };
        taskIdColumn.setSortable( true );
        extendedPagedTable.addColumn(taskIdColumn, constants.Id());
        taskIdColumn.setDataStoreName( "Id" );
    }

    private void initJobTypeColumn(ExtendedPagedTable extendedPagedTable){
        // Name
        Column<RequestSummary, String> jobTypeColumn = new Column<RequestSummary, String>( new TextCell() ) {
            @Override
            public String getValue( RequestSummary object ) {
                return object.getCommandName();
            }
        };
        jobTypeColumn.setSortable( true );
        extendedPagedTable.addColumn(jobTypeColumn, constants.Type());
        jobTypeColumn.setDataStoreName( "CommandName" );
    }

    private void initStatusColumn(ExtendedPagedTable extendedPagedTable){
        // Status
        Column<RequestSummary, String> statusColumn = new Column<RequestSummary, String>( new TextCell() ) {
            @Override
            public String getValue( RequestSummary object ) {
                return object.getStatus();
            }
        };
        statusColumn.setSortable( true );
        extendedPagedTable.addColumn(statusColumn, constants.Status());
        statusColumn.setDataStoreName( "Status" );
    }

    private void initDueDateColumn(ExtendedPagedTable extendedPagedTable){
        // Time
        Column<RequestSummary, String> taskNameColumn = new Column<RequestSummary, String>( new TextCell() ) {
            @Override
            public String getValue( RequestSummary object ) {
                return object.getTime().toString();
            }
        };
        taskNameColumn.setSortable( true );
        extendedPagedTable.addColumn(taskNameColumn, constants.Due_On());
        taskNameColumn.setDataStoreName( "Time" );
    }

    private Column<RequestSummary, RequestSummary> initActionsColumn(){
        List<HasCell<RequestSummary, ?>> cells = new LinkedList<HasCell<RequestSummary, ?>>();
        List<String> allStatuses = new ArrayList<String>();
        allStatuses.add("QUEUED");
        allStatuses.add("DONE");
        allStatuses.add("CANCELLED");
        allStatuses.add("ERROR");
        allStatuses.add("RETRYING");
        allStatuses.add("RUNNING");
        cells.add( new ActionHasCell( "Details", allStatuses, new Delegate<RequestSummary>() {
            @Override
            public void execute( RequestSummary job ) {
                jobDetailsPopup.show(String.valueOf( job.getJobId() ));
            }
        } ) );

        List<String> activeStatuses = new ArrayList<String>();
        activeStatuses.add("QUEUED");
        activeStatuses.add("RETRYING");
        activeStatuses.add("RUNNING");
        cells.add( new ActionHasCell( "Cancel", activeStatuses, new Delegate<RequestSummary>() {
            @Override
            public void execute( RequestSummary job ) {
                if ( Window.confirm( "Are you sure you want to cancel this Job?" ) ) {
                    presenter.cancelRequest( job.getJobId() );
                }
            }
        } ) );

        List<String> requeueStatuses = new ArrayList<String>();
        requeueStatuses.add("ERROR");
        requeueStatuses.add("RUNNING");
        cells.add( new ActionHasCell( "Requeue", requeueStatuses, new Delegate<RequestSummary>() {
            @Override
            public void execute( RequestSummary job ) {
                if ( Window.confirm( "Are you sure you want to requeue this Job?" ) ) {
                    presenter.requeueRequest(job.getJobId());
                }
            }
        } ) );

        CompositeCell<RequestSummary> cell = new CompositeCell<RequestSummary>( cells );
        Column<RequestSummary, RequestSummary> actionsColumn = new Column<RequestSummary, RequestSummary>( cell ) {
            @Override
            public RequestSummary getValue( RequestSummary object ) {
                return object;
            }
        };
        return actionsColumn;
    }

//    @Override
//    public void displayNotification( String text ) {
//        notification.fire( new NotificationEvent( text ) );
//    }

    private class ActionHasCell implements HasCell<RequestSummary, RequestSummary> {

        private final List<String> avaliableStatuses;
        private ActionCell<RequestSummary> cell;

        public ActionHasCell( String text, List<String> avaliableStatusesList,
                              Delegate<RequestSummary> delegate ) {
            this.avaliableStatuses = avaliableStatusesList;
            cell = new ActionCell<RequestSummary>( text, delegate ){
                @Override
                public void render(Context context, RequestSummary value, SafeHtmlBuilder sb) {
                    if (avaliableStatuses.contains(value.getStatus())){
                        super.render(context, value, sb);
                    }
                }
            };
        }

        @Override
        public Cell<RequestSummary> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<RequestSummary, RequestSummary> getFieldUpdater() {
            return null;
        }

        @Override
        public RequestSummary getValue( RequestSummary object ) {
            return object;
        }
    }

    public  void initSelectionModel (final ExtendedPagedTable additionalGrid,final String key){

        this.initActionsDropDown(additionalGrid);
        additionalGrid.setEmptyTableCaption( constants.No_Jobs_Found() );
        initSelectionModel(additionalGrid);
        initNoActionColumnManager(additionalGrid);

        additionalGrid.setSelectionModel( selectionModel, noActionColumnManager );
        additionalGrid.setRowStyles( selectedStyles );
    }

}
