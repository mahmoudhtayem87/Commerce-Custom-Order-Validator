package com.liferay.me.internal.commerce.order;

import com.liferay.commerce.model.CommerceOrder;
import com.liferay.commerce.model.CommerceOrderItem;
import com.liferay.commerce.order.CommerceOrderValidator;
import com.liferay.commerce.order.CommerceOrderValidatorResult;
import com.liferay.commerce.product.model.CPInstance;
import com.liferay.expando.kernel.model.ExpandoBridge;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.io.Serializable;
import java.math.BigDecimal;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	property = {
		"commerce.order.validator.key=CustomCommerceOrderValidator-001",
		"commerce.order.validator.priority:Integer=9"
	},
	service = CommerceOrderValidator.class
)
public class CustomCommerceOrderValidator implements CommerceOrderValidator {

	@Override
	public String getKey() {
		return "CustomCommerceOrderValidator-001";
	}

	@Override
	public CommerceOrderValidatorResult validate(
			Locale locale, CommerceOrder commerceOrder, CPInstance cpInstance,
			int quantity)
		throws PortalException {
		long userId = commerceOrder.getUserId();
		User ownerUser = userLocalService.getUser(userId);
		ExpandoBridge expandoBridge = ownerUser.getExpandoBridge();
		Serializable temp_expando_budget = expandoBridge.getAttribute("Budget",true);
		BigDecimal Budget = BigDecimal.valueOf((Double) temp_expando_budget);
		if (cpInstance == null) {
			return new CommerceOrderValidatorResult(false);
		}
		List<CommerceOrderItem> items =
		commerceOrder.getCommerceOrderItems();
		boolean AvailableInCart= false;
		//to be total price!
		BigDecimal total = BigDecimal.valueOf(0);
		for (int index = 0 ; index < items.stream().count();index++)
		{
			BigDecimal sub_total = BigDecimal.valueOf(0);
			if (items.get(index).getCPDefinitionId()  == cpInstance.getCPDefinitionId())
			{
				AvailableInCart = true;
				sub_total = items.get(index).getUnitPrice().multiply(BigDecimal.valueOf(quantity));
			}
			else
			{
				sub_total = items.get(index).getUnitPrice()
						.multiply(BigDecimal.valueOf(items.get(index).getQuantity()));
			}
			total = total.add(sub_total);
		}
		if(!AvailableInCart)
		{
			BigDecimal sub_total = BigDecimal.valueOf(0);
			sub_total = cpInstance.getPrice()
					.multiply(BigDecimal.valueOf(quantity));
			total = total.add(sub_total);
		}
		BigDecimal price = cpInstance.getPrice();
		if (Budget.compareTo(total) == 1 || Budget.compareTo(total) == 0 ) {
			return new CommerceOrderValidatorResult(true);
		}
		else
		{
			ResourceBundle resourceBundle = ResourceBundleUtil.getBundle(
					"content.Language", locale, getClass());
			return new CommerceOrderValidatorResult(
					false,
					LanguageUtil.format(
							resourceBundle,
							"this-expensive-item-has-a-maximum-quantity-of-x",
							Budget));
		}
	}

	@Override
	public CommerceOrderValidatorResult validate(
			Locale locale, CommerceOrderItem commerceOrderItem)
		throws PortalException {
		return new CommerceOrderValidatorResult(true);
	}
	@Reference
	protected com.liferay.portal.kernel.service.UserLocalService
			userLocalService;

}