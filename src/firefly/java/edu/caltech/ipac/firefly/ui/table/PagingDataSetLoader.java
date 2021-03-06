/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.DataSet;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.util.DataSetParser;
import edu.caltech.ipac.firefly.util.WebUtil;

import java.util.Map;


/**
 * Date: Feb 11, 2009
 *
 * @author loi
 * @version $Id: PagingDataSetLoader.java,v 1.23 2012/10/26 14:39:18 tatianag Exp $
 */
public abstract class PagingDataSetLoader extends AbstractLoader<TableDataView> {
    private TableServerRequest request;
    boolean isStandAlone = false;

    protected PagingDataSetLoader(TableServerRequest request) {
        this.request = request;
//        setPageSize(request.getPageSize());
//        setFilters(request.getFilters());
//        setSortInfo(request.getSortInfo());
    }

    /**
     *
     * @param req
     * @param callback
     */
    public void getData(TableServerRequest req, final AsyncCallback<TableDataView> callback) {

        AsyncCallback<RawDataSet> cb = new AsyncCallback<RawDataSet>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(RawDataSet result) {
                DataSet dataset = DataSetParser.parse(result);
                callback.onSuccess(dataset);
            }
        };
        doLoadData(req, cb);
    }

    public void load(int offset, int pageSize, final AsyncCallback<TableDataView> callback) {

        request.setStartIndex(offset);
        request.setPageSize(pageSize);
        request.setFilters(getFilters());
        request.setSortInfo(getSortInfo());

        AsyncCallback<TableDataView> cb = new AsyncCallback<TableDataView>() {
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            public void onSuccess(TableDataView result) {
                if(getCurrentData() == null) {
                    setCurrentData(result);
                }
                TableDataView tv = getCurrentData();
                tv.setModel(result.getModel());
                tv.setTotalRows(result.getTotalRows());
                tv.setStartingIdx(result.getStartingIdx());

                if (result.getMeta() != null) {
                    Map<String, String> attribs = tv.getMeta().getAttributes();
                    tv.setMeta(result.getMeta());
                    if (attribs != null) {
                        for(String k : attribs.keySet()) {
                            tv.getMeta().setAttribute(k, attribs.get(k));
                        }
                    }

                }
                onLoad(result);
                callback.onSuccess(result);
            }
        };
        getData(request, cb);
    }

    public TableServerRequest getRequest() {
        return request;
    }

    @Override
    public void onLoad(TableDataView result) {
        // do not replace the tableview
    }

    public String getSourceUrl() {
        // sets start index to 0, page size to infinity
        return WebUtil.getTableSourceUrl(request);
    }

    protected abstract void doLoadData(TableServerRequest request, AsyncCallback<RawDataSet> callback);

}
