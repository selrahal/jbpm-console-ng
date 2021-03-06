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
package org.jbpm.console.ng.pr.client.editors.instance.list;

import com.github.gwtbootstrap.client.ui.*;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.*;
import com.google.gwt.cell.client.ActionCell.Delegate;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.jbpm.console.ng.gc.client.experimental.grid.base.ExtendedPagedTable;
import org.jbpm.console.ng.gc.client.list.base.AbstractListView;
import org.jbpm.console.ng.pr.client.i18n.Constants;
import org.jbpm.console.ng.pr.client.resources.ProcessRuntimeImages;
import org.jbpm.console.ng.pr.model.ProcessInstanceSummary;
import org.jbpm.console.ng.pr.model.events.ProcessInstanceSelectionEvent;
import org.jbpm.console.ng.pr.model.events.ProcessInstancesWithDetailsRequestEvent;
import org.kie.api.runtime.process.ProcessInstance;
import org.uberfire.client.mvp.PlaceStatus;
import org.uberfire.client.workbench.events.BeforeClosePlaceEvent;
import org.uberfire.ext.services.shared.preferences.GridGlobalPreferences;
import org.uberfire.ext.widgets.common.client.tables.ColumnMeta;
import org.uberfire.ext.widgets.common.client.tables.DataGridFilter;
import org.uberfire.ext.widgets.common.client.tables.popup.NewFilterPopup;
import org.uberfire.mvp.Command;
import org.uberfire.mvp.PlaceRequest;
import org.uberfire.mvp.impl.DefaultPlaceRequest;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import java.util.*;

@Dependent
public class ProcessInstanceListViewImpl extends AbstractListView<ProcessInstanceSummary, ProcessInstanceListPresenter>
        implements ProcessInstanceListPresenter.ProcessInstanceListView {

    interface Binder
            extends
            UiBinder<Widget, ProcessInstanceListViewImpl> {

    }

    private static Binder uiBinder = GWT.create( Binder.class );

    private Constants constants = GWT.create( Constants.class );
    private ProcessRuntimeImages images = GWT.create( ProcessRuntimeImages.class );

    private List<ProcessInstanceSummary> selectedProcessInstances = new ArrayList<ProcessInstanceSummary>();

    @Inject
    private Event<ProcessInstanceSelectionEvent> processInstanceSelected;

    @Inject
    private NewFilterPopup newFilterPopup;

    private Column actionsColumn;

    private NavLink bulkAbortNavLink;
    private NavLink bulkSignalNavLink;

    private void controlBulkOperations() {
        if ( selectedProcessInstances != null && selectedProcessInstances.size() > 0 ) {
            bulkAbortNavLink.setDisabled( false );
            bulkSignalNavLink.setDisabled( false );
        } else {
            bulkAbortNavLink.setDisabled( true );
            bulkSignalNavLink.setDisabled( true );
        }
    }

    @Override
    public void init( final ProcessInstanceListPresenter presenter ) {
        final List<String> bannedColumns = new ArrayList<String>();
        bannedColumns.add( constants.Select() );
        bannedColumns.add( constants.Id() );
        bannedColumns.add( constants.Name() );
        bannedColumns.add( constants.Process_Instance_Description() );
        bannedColumns.add( constants.Actions() );
        final List<String> initColumns = new ArrayList<String>();
        initColumns.add( constants.Select() );
        initColumns.add( constants.Id() );
        initColumns.add( constants.Name() );
        initColumns.add( constants.Process_Instance_Description() );
        initColumns.add( constants.Version() );
        initColumns.add( constants.Actions() );
        initColumns.add( constants.Version() );
        super.init( presenter, new GridGlobalPreferences( "ProcessInstancesGrid", initColumns, bannedColumns ) );
        initSelectionModel( listGrid );
    }


    public void initSelectionModel (final ExtendedPagedTable additionalGrid){
        initBulkActionsDropDown(additionalGrid);
        additionalGrid.setEmptyTableCaption( constants.No_Process_Instances_Found() );

        final ExtendedPagedTable extendedPagedTable = additionalGrid;
        selectionModel = new NoSelectionModel<ProcessInstanceSummary>();
        selectionModel.addSelectionChangeHandler( new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange( SelectionChangeEvent event ) {

                boolean close = false;
                if ( selectedRow == -1 ) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();

                } else if ( extendedPagedTable.getKeyboardSelectedRow() != selectedRow ) {
                    extendedPagedTable.setRowStyles( selectedStyles );
                    selectedRow = extendedPagedTable.getKeyboardSelectedRow();
                    extendedPagedTable.redraw();
                } else {
                    close = true;
                }

                selectedItem = selectionModel.getLastSelectedObject();

                PlaceStatus status = placeManager.getStatus( new DefaultPlaceRequest( "Process Instance Details Multi" ) );

                if ( status == PlaceStatus.CLOSE ) {
                    placeManager.goTo( "Process Instance Details Multi" );
                    processInstanceSelected.fire( new ProcessInstanceSelectionEvent( selectedItem.getDeploymentId(),
                            selectedItem.getProcessInstanceId(), selectedItem.getProcessId(),
                            selectedItem.getProcessName(), selectedItem.getState() ) );
                } else if ( status == PlaceStatus.OPEN && !close ) {
                    processInstanceSelected.fire( new ProcessInstanceSelectionEvent( selectedItem.getDeploymentId(),
                            selectedItem.getProcessInstanceId(), selectedItem.getProcessId(),
                            selectedItem.getProcessName(), selectedItem.getState() ) );
                } else if ( status == PlaceStatus.OPEN && close ) {
                    placeManager.closePlace( "Process Instance Details Multi" );
                }

            }
        } );

        noActionColumnManager = DefaultSelectionEventManager
                .createCustomManager( new DefaultSelectionEventManager.EventTranslator<ProcessInstanceSummary>() {

                    @Override
                    public boolean clearCurrentSelection( CellPreviewEvent<ProcessInstanceSummary> event ) {
                        return false;
                    }

                    @Override
                    public DefaultSelectionEventManager.SelectAction translateSelectionEvent( CellPreviewEvent<ProcessInstanceSummary> event ) {
                        NativeEvent nativeEvent = event.getNativeEvent();
                        if ( BrowserEvents.CLICK.equals( nativeEvent.getType() ) ) {
                            // Ignore if the event didn't occur in the correct column.
                            if ( extendedPagedTable.getColumnIndex( actionsColumn ) == event.getColumn() ) {
                                return DefaultSelectionEventManager.SelectAction.IGNORE;
                            }
                            //Extension for checkboxes
                            Element target = nativeEvent.getEventTarget().cast();
                            if ( "input".equals( target.getTagName().toLowerCase() ) ) {
                                final InputElement input = target.cast();
                                if ( "checkbox".equals( input.getType().toLowerCase() ) ) {
                                    // Synchronize the checkbox with the current selection state.
                                    if ( !selectedProcessInstances.contains( event.getValue() ) ) {
                                        selectedProcessInstances.add( event.getValue() );
                                        input.setChecked( true );
                                    } else {
                                        selectedProcessInstances.remove( event.getValue() );
                                        input.setChecked( false );
                                    }
                                    controlBulkOperations();
                                    return DefaultSelectionEventManager.SelectAction.IGNORE;
                                }
                            }
                        }

                        return DefaultSelectionEventManager.SelectAction.DEFAULT;
                    }

                } );

        additionalGrid.setSelectionModel( selectionModel, noActionColumnManager );
        additionalGrid.setRowStyles( selectedStyles );
    }

    @Override
    public void initColumns(  ) {
        Column checkColumn = initChecksColumn();
        Column processInstanceIdColumn = initProcessInstanceIdColumn();
        Column processNameColumn = initProcessNameColumn();
        Column processInitiatorColumn = initInitiatorColumn();
        Column processVersionColumn = initProcessVersionColumn();
        Column processStateColumn = initProcessStateColumn();
        Column startTimeColumn = initStartDateColumn();
        Column descriptionColumn = initDescriptionColumn();
        actionsColumn = initActionsColumn();

        List<ColumnMeta<ProcessInstanceSummary>> columnMetas = new ArrayList<ColumnMeta<ProcessInstanceSummary>>();
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( checkColumn, constants.Select() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( processInstanceIdColumn, constants.Id() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( processNameColumn, constants.Name() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( descriptionColumn, constants.Process_Instance_Description() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( processInitiatorColumn, constants.Initiator() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( processVersionColumn, constants.Version() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( processStateColumn, constants.State() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( startTimeColumn, constants.Start_Date() ) );
        columnMetas.add( new ColumnMeta<ProcessInstanceSummary>( actionsColumn, constants.Actions() ) );
        listGrid.addColumns( columnMetas );
    }

    @Override
    public void initFilters(  ) {
        listGrid.setShowFilterSelector( true );

        listGrid.addFilter( new DataGridFilter<ProcessInstanceSummary>( "active", Constants.INSTANCE.Active(),
                new Command() {
                    @Override
                    public void execute() {
                        presenter.refreshActiveProcessList(  );
                    }
                } ) );

        listGrid.addFilter( new DataGridFilter<ProcessInstanceSummary>( "completed", Constants.INSTANCE.Completed(),
                new Command() {
                    @Override
                    public void execute() {
                        presenter.refreshCompletedProcessList( );
                    }
                } ) );

        listGrid.addFilter( new DataGridFilter<ProcessInstanceSummary>( "aborted", Constants.INSTANCE.Aborted(),
                new Command() {
                    @Override
                    public void execute() {
                        presenter.refreshAbortedProcessList( );
                    }
                } ) );

        listGrid.addFilter( new DataGridFilter<ProcessInstanceSummary>( "relatedToMe", constants.Related_To_Me(),
                new Command() {
                    @Override
                    public void execute() {
                        presenter.refreshRelatedToMeProcessList(  identity.getIdentifier() );

                    }
                } ) );
        final HashMap storedCustomFilters = listGrid.getStoredCustomFilters();
        List<DataGridFilter> customFilters = new ArrayList<DataGridFilter>();
        if ( storedCustomFilters != null ) {
            Set customFilterKeys = storedCustomFilters.keySet();
            Iterator it = customFilterKeys.iterator();
            String customFilterName;
            DataGridFilter dataGridFilter;
            while ( it.hasNext() ) {
                customFilterName = ( String ) it.next();

                final HashMap filterValues = ( HashMap ) storedCustomFilters.get( customFilterName );
                dataGridFilter = new DataGridFilter<ProcessInstanceSummary>( customFilterName, customFilterName,
                        new Command() {
                            @Override
                            public void execute() {
                                List<String> states = ( List ) filterValues.get( ProcessInstanceListPresenter.FILTER_STATE_PARAM_NAME );
                                ArrayList<Integer> statesInteger = new ArrayList<Integer>();
                                for ( String state : states ) {
                                    statesInteger.add( Integer.parseInt( state ) );
                                }
                                presenter.filterGrid(  statesInteger, ( String ) filterValues.get( ProcessInstanceListPresenter.FILTER_PROCESS_DEFINITION_PARAM_NAME ),
                                        ( String ) filterValues.get( ProcessInstanceListPresenter.FILTER_INITIATOR_PARAM_NAME ));
                            }
                        } );
                listGrid.addFilter( dataGridFilter );
                customFilters.add( dataGridFilter );

            }
        }
        final Command refreshFilterDropDownCommand = new Command() {
            @Override
            public void execute() {
                listGrid.clearFilters();
                initFilters( );
            }
        };
        listGrid.addFilter( new DataGridFilter<ProcessInstanceSummary>( "addFilter", "-- " + Constants.INSTANCE.FilterManagement() + " --",
                new Command() {
                    @Override
                    public void execute() {
                        Command addFilter = new Command() {
                            @Override
                            public void execute() {
                                final String newFilterName = ( String ) newFilterPopup.getFormValues().get( NewFilterPopup.FILTER_NAME_PARAM );
                                listGrid.storeNewCustomFilter( newFilterName, newFilterPopup.getFormValues() );
                                listGrid.clearFilters();
                                initFilters( );
                            }
                        };
                        createFilterForm();
                        newFilterPopup.show( addFilter, refreshFilterDropDownCommand, listGrid.getGridPreferencesStore() );
                    }
                } ) );
        listGrid.refreshFilterDropdown();

    }


    private void createFilterForm() {
        HashMap<String, String> stateListBoxInfo = new HashMap<String, String>();

        stateListBoxInfo.put( String.valueOf( ProcessInstance.STATE_ACTIVE ), Constants.INSTANCE.Active() );
        stateListBoxInfo.put( String.valueOf( ProcessInstance.STATE_COMPLETED ), Constants.INSTANCE.Completed() );
        stateListBoxInfo.put( String.valueOf( ProcessInstance.STATE_ABORTED ), Constants.INSTANCE.Aborted() );
        stateListBoxInfo.put( String.valueOf( ProcessInstance.STATE_PENDING ), Constants.INSTANCE.Pending() );
        stateListBoxInfo.put( String.valueOf( ProcessInstance.STATE_SUSPENDED ), Constants.INSTANCE.Suspended() );

        newFilterPopup.init();
        newFilterPopup.addListBoxToFilter( Constants.INSTANCE.State(), ProcessInstanceListPresenter.FILTER_STATE_PARAM_NAME, true, stateListBoxInfo );
        newFilterPopup.addTextBoxToFilter( Constants.INSTANCE.Process_Definitions(), ProcessInstanceListPresenter.FILTER_PROCESS_DEFINITION_PARAM_NAME );
        newFilterPopup.addTextBoxToFilter( Constants.INSTANCE.Initiator(), ProcessInstanceListPresenter.FILTER_INITIATOR_PARAM_NAME );

    }

    private void initBulkActionsDropDown( final ExtendedPagedTable extendedPagedTable ) {
        SplitDropdownButton bulkActions = new SplitDropdownButton();
        bulkActions.setText( constants.Bulk_Actions() );
        bulkAbortNavLink = new NavLink( constants.Bulk_Abort() );
        bulkAbortNavLink.setIcon( IconType.REMOVE_SIGN );
        bulkAbortNavLink.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                presenter.bulkAbort( selectedProcessInstances );
                selectedProcessInstances.clear();
                extendedPagedTable.redraw();
            }
        } );

        bulkSignalNavLink = new NavLink( constants.Bulk_Signal() );
        bulkSignalNavLink.setIcon( IconType.BELL );
        bulkSignalNavLink.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                presenter.bulkSignal( selectedProcessInstances );
                selectedProcessInstances.clear();
                extendedPagedTable.redraw();
            }
        } );

        bulkActions.add( bulkAbortNavLink );
        bulkActions.add( bulkSignalNavLink );
        extendedPagedTable.getLeftToolbar().add( bulkActions );

        controlBulkOperations();
    }

    private Column initProcessInstanceIdColumn() {
        // Process Instance Id.
        Column<ProcessInstanceSummary, String> processInstanceIdColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                return String.valueOf( object.getProcessInstanceId() );
            }
        };
        processInstanceIdColumn.setSortable( true );
        processInstanceIdColumn.setDataStoreName( "log.processInstanceId" );

        return processInstanceIdColumn;
    }

    private Column initProcessNameColumn() {
        // Process Name.
        Column<ProcessInstanceSummary, String> processNameColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                return object.getProcessName();
            }
        };
        processNameColumn.setSortable( true );
        processNameColumn.setDataStoreName( "log.processName" );

        return processNameColumn;
    }

    private Column initInitiatorColumn() {
        Column<ProcessInstanceSummary, String> processInitiatorColumn = new Column<ProcessInstanceSummary, String>(
                new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                return object.getInitiator();
            }
        };
        processInitiatorColumn.setSortable( true );
        processInitiatorColumn.setDataStoreName( "log.identity" );

        return processInitiatorColumn;
    }

    private Column initProcessVersionColumn() {
        // Process Version.
        Column<ProcessInstanceSummary, String> processVersionColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                return object.getProcessVersion();
            }
        };
        processVersionColumn.setSortable( true );
        processVersionColumn.setDataStoreName( "log.processVersion" );

        return processVersionColumn;
    }

    private Column initProcessStateColumn() {
        // Process State
        Column<ProcessInstanceSummary, String> processStateColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                String statusStr = constants.Unknown();
                switch ( object.getState() ) {
                    case ProcessInstance.STATE_ACTIVE:
                        statusStr = constants.Active();
                        break;
                    case ProcessInstance.STATE_ABORTED:
                        statusStr = constants.Aborted();
                        break;
                    case ProcessInstance.STATE_COMPLETED:
                        statusStr = constants.Completed();
                        break;
                    case ProcessInstance.STATE_PENDING:
                        statusStr = constants.Pending();
                        break;
                    case ProcessInstance.STATE_SUSPENDED:
                        statusStr = constants.Suspended();
                        break;

                    default:
                        break;
                }

                return statusStr;
            }
        };
        processStateColumn.setSortable( true );
        processStateColumn.setDataStoreName( "log.status" );

        return processStateColumn;
    }

    private Column initStartDateColumn() {
        // start time
        Column<ProcessInstanceSummary, String> startTimeColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                Date startTime = object.getStartTime();
                if ( startTime != null ) {
                    DateTimeFormat format = DateTimeFormat.getFormat( "dd/MM/yyyy HH:mm" );
                    return format.format( startTime );
                }
                return "";
            }
        };
        startTimeColumn.setSortable( true );
        startTimeColumn.setDataStoreName( "log.start" );

        return startTimeColumn;
    }

    private Column initActionsColumn() {
        List<HasCell<ProcessInstanceSummary, ?>> cells = new LinkedList<HasCell<ProcessInstanceSummary, ?>>();

        cells.add( new SignalActionHasCell( constants.Signal(), new Delegate<ProcessInstanceSummary>() {
            @Override
            public void execute( ProcessInstanceSummary processInstance ) {

                PlaceRequest placeRequestImpl = new DefaultPlaceRequest( "Signal Process Popup" );
                placeRequestImpl.addParameter( "processInstanceId", Long.toString( processInstance.getProcessInstanceId() ) );

                placeManager.goTo( placeRequestImpl );
            }
        } ) );

        cells.add( new AbortActionHasCell( constants.Abort(), new Delegate<ProcessInstanceSummary>() {
            @Override
            public void execute( ProcessInstanceSummary processInstance ) {
                if ( Window.confirm( "Are you sure that you want to abort the process instance?" ) ) {
                    presenter.abortProcessInstance( processInstance.getProcessInstanceId() );
                }
            }
        } ) );

        CompositeCell<ProcessInstanceSummary> cell = new CompositeCell<ProcessInstanceSummary>( cells );
        Column<ProcessInstanceSummary, ProcessInstanceSummary> actionsColumn = new Column<ProcessInstanceSummary, ProcessInstanceSummary>(
                cell ) {
            @Override
            public ProcessInstanceSummary getValue( ProcessInstanceSummary object ) {
                return object;
            }
        };
        return actionsColumn;

    }

    private Column initChecksColumn() {
        // Checkbox column. This table will uses a checkbox column for selection.
        // Alternatively, you can call dataGrid.setSelectionEnabled(true) to enable
        // mouse selection.
        Column<ProcessInstanceSummary, Boolean> checkColumn = new Column<ProcessInstanceSummary, Boolean>( new CheckboxCell(
                true, false ) ) {
            @Override
            public Boolean getValue( ProcessInstanceSummary object ) {
                // Get the value from the selection model.
                return selectedProcessInstances.contains( object );
            }
        };

        return checkColumn;
    }

    private Column initDescriptionColumn() {
        // start time
        Column<ProcessInstanceSummary, String> descriptionColumn = new Column<ProcessInstanceSummary, String>( new TextCell() ) {
            @Override
            public String getValue( ProcessInstanceSummary object ) {
                return object.getProcessInstanceDescription();
            }
        };
        descriptionColumn.setSortable( true );
        descriptionColumn.setDataStoreName( "log.processInstanceDescription" );
        return descriptionColumn;
    }

    public void onProcessInstanceSelectionEvent( @Observes ProcessInstancesWithDetailsRequestEvent event ) {
        placeManager.goTo( "Process Instance Details Multi" );
        processInstanceSelected.fire( new ProcessInstanceSelectionEvent( event.getDeploymentId(),
                event.getProcessInstanceId(), event.getProcessDefId(),
                event.getProcessDefName(), event.getProcessInstanceStatus() ) );
    }

    private class AbortActionHasCell implements HasCell<ProcessInstanceSummary, ProcessInstanceSummary> {

        private ActionCell<ProcessInstanceSummary> cell;

        public AbortActionHasCell( String text,
                                   Delegate<ProcessInstanceSummary> delegate ) {
            cell = new ActionCell<ProcessInstanceSummary>( text, delegate ) {
                @Override
                public void render( Cell.Context context,
                                    ProcessInstanceSummary value,
                                    SafeHtmlBuilder sb ) {
                    if ( value.getState() == ProcessInstance.STATE_ACTIVE ) {
                        AbstractImagePrototype imageProto = AbstractImagePrototype.create( images.abortGridIcon() );
                        SafeHtmlBuilder mysb = new SafeHtmlBuilder();
                        mysb.appendHtmlConstant( "<span title='" + constants.Abort() + "' style='margin-right:5px;'>" );
                        mysb.append( imageProto.getSafeHtml() );
                        mysb.appendHtmlConstant( "</span>" );
                        sb.append( mysb.toSafeHtml() );
                    }
                }
            };
        }

        @Override
        public Cell<ProcessInstanceSummary> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<ProcessInstanceSummary, ProcessInstanceSummary> getFieldUpdater() {
            return null;
        }

        @Override
        public ProcessInstanceSummary getValue( ProcessInstanceSummary object ) {
            return object;
        }
    }

    private class SignalActionHasCell implements HasCell<ProcessInstanceSummary, ProcessInstanceSummary> {

        private ActionCell<ProcessInstanceSummary> cell;

        public SignalActionHasCell( String text,
                                    Delegate<ProcessInstanceSummary> delegate ) {
            cell = new ActionCell<ProcessInstanceSummary>( text, delegate ) {
                @Override
                public void render( Cell.Context context,
                                    ProcessInstanceSummary value,
                                    SafeHtmlBuilder sb ) {
                    if ( value.getState() == ProcessInstance.STATE_ACTIVE ) {
                        AbstractImagePrototype imageProto = AbstractImagePrototype.create( images.signalGridIcon() );
                        SafeHtmlBuilder mysb = new SafeHtmlBuilder();
                        mysb.appendHtmlConstant( "<span title='" + constants.Signal() + "' style='margin-right:5px;'>" );
                        mysb.append( imageProto.getSafeHtml() );
                        mysb.appendHtmlConstant( "</span>" );
                        sb.append( mysb.toSafeHtml() );
                    }
                }
            };
        }

        @Override
        public Cell<ProcessInstanceSummary> getCell() {
            return cell;
        }

        @Override
        public FieldUpdater<ProcessInstanceSummary, ProcessInstanceSummary> getFieldUpdater() {
            return null;
        }

        @Override
        public ProcessInstanceSummary getValue( ProcessInstanceSummary object ) {
            return object;
        }
    }

    public void formClosed( @Observes BeforeClosePlaceEvent closed ) {
        if ( "Signal Process Popup".equals( closed.getPlace().getIdentifier() ) ) {
            presenter.refreshActiveProcessList( );
        }
    }
}
