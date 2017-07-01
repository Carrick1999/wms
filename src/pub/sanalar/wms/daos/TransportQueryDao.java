package pub.sanalar.wms.daos;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;
import org.springframework.transaction.annotation.Transactional;

import pub.sanalar.wms.models.TransportSubmitItem;
import pub.sanalar.wms.models.WmsDispatch;
import pub.sanalar.wms.models.WmsDispatchProductShelf;
import pub.sanalar.wms.models.WmsDispatchState;
import pub.sanalar.wms.models.WmsProduct;
import pub.sanalar.wms.models.WmsProductShelf;
import pub.sanalar.wms.models.WmsShelf;
import pub.sanalar.wms.models.WmsUser;
import pub.sanalar.wms.models.WmsWarehouse;

@Transactional
public class TransportQueryDao extends HibernateDaoSupport {

	public String newTransport(Integer userId, Integer fromWId, Integer toWId, 
			List<TransportSubmitItem> items, String desc){
		WmsUser user = getHibernateTemplate().get(WmsUser.class, userId);
		if(user == null){
			return "�û���Ϣ��Ч���޷���ӣ�";
		}
		
		WmsWarehouse fromHouse = getHibernateTemplate().get(WmsWarehouse.class, fromWId);
		WmsWarehouse toHouse = getHibernateTemplate().get(WmsWarehouse.class, toWId);
		if(fromHouse == null || toHouse == null){
			return "��Ч�Ĳֿ���Ϣ��";
		}
		
		// �����Ҫ��ӵ���Ϣ�Ƿ�Ϸ�
		HashMap<Integer, Integer> trans = new HashMap<Integer, Integer>();
		for(TransportSubmitItem i : items){
			if(trans.containsKey(i.getId())){
				trans.put(i.getId(), trans.get(i.getId()) + i.getNumber());
			}else{
				trans.put(i.getId(), i.getNumber());
			}
		}
		for(Integer i : trans.keySet()){
			WmsProductShelf sh = getHibernateTemplate().get(WmsProductShelf.class, i);
			if(sh == null){
				return "��Ч���б������";
			}
			if(sh.getWmsShelf().getWmsStorage().getWmsWarehouse().getWarehouseId() != fromWId){
				return "��ͼת��һ���������Լ��Ŀ�λ��Ļ��";
			}
			if(sh.getPsNumber() < trans.get(i)){
				return "��" + sh.getWmsShelf().getShelfName() + "�������д�ŵġ�" + sh.getWmsProduct().getProductName()
						+ "�������������������ε��ȡ���ŵ�����Ϊ��" + sh.getPsNumber() + "����Ҫ���ȵ�����Ϊ��" + trans.get(i);
			}
		}
		
		// ���ͨ������ʼ��Ӽ�¼
		Session session = getSessionFactory().getCurrentSession();
		WmsDispatch dispatch = new WmsDispatch();
		Calendar cl = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		dispatch.setDispatchId("WMS-DISP-" + sdf.format(cl.getTime()));
		dispatch.setDispatchAcceptTime(null);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time = df.format(cl.getTime());
		dispatch.setDispatchCreateTime(Timestamp.valueOf(time));
		dispatch.setDispatchDescription(desc);
		dispatch.setDispatchFinishTime(null);
		dispatch.setWmsDispatchState(
				getHibernateTemplate().get(WmsDispatchState.class, 1)
				);
		dispatch.setWmsUserByDispatchAccpetor(null);
		dispatch.setWmsUserByDispatchCreator(user);
		dispatch.setWmsUserByDispatchFinisher(null);
		dispatch.setWmsWarehouseByDispatchFrom(fromHouse);
		dispatch.setWmsWarehouseByDispatchTo(toHouse);
		session.save(dispatch);
		for(TransportSubmitItem i : items){
			WmsDispatchProductShelf s = new WmsDispatchProductShelf();
			s.setDpsCost(i.getCost());
			s.setDpsNumber(i.getNumber());
			s.setWmsDispatch(dispatch);
			s.setWmsProduct(
					getHibernateTemplate().get(WmsProduct.class, i.getProductId())
					);
			s.setWmsShelf(
					getHibernateTemplate().get(WmsShelf.class, i.getShelfId())
					);
			session.save(s);
		}
		
		session.flush();
		return "�ύ�µĵ�����������ɹ���";
	}
}
