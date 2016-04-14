package org.es.plugin.product;

import java.util.Map;

import org.elasticsearch.index.fielddata.ScriptDocValues;
import org.elasticsearch.script.AbstractDoubleSearchScript;

import com.sun.istack.internal.Nullable;

public class CustomScript extends AbstractDoubleSearchScript {
	private double price;
    private String category;
	 public CustomScript(@Nullable Map<String,Object>params) {
		 this.price = (Double)params.get("price");
       this.category = (String)params.get("category");
	}
	@Override
	public double runAsDouble() {
		long iismerchant = ((ScriptDocValues.Longs)doc().get("iismerchant")).getValue();
        double score = 30;
        if(iismerchant == 1) {
            score += 20;
        }
        else {
            score += this.price;
        }
        score = score < 0 ? 0 : score;
        return score;
	}

}
