package com.tariffsheriff.backend.tariff.service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.tariffsheriff.backend.tariff.dto.TariffRateRequestDto;
import com.tariffsheriff.backend.tariff.exception.TariffRateNotFoundException;
import com.tariffsheriff.backend.tariff.model.TariffRate;
import com.tariffsheriff.backend.tariff.repository.TariffRateRepository;

@Service
public class TariffRateServiceImpl implements TariffRateService {
    private TariffRateRepository tariffRates;

    public TariffRateServiceImpl(TariffRateRepository tariffRates){
        this.tariffRates = tariffRates;
    }

    public List<TariffRate> listTariffRates() {
        return tariffRates.findAll();
    }

    public TariffRate getTariffRateById(Long id) {
        return tariffRates.findById(id).map(tariffRate -> {
            return tariffRate;
        }).orElseThrow(() -> new TariffRateNotFoundException());
    }

    public TariffRate getTariffRateByImporterAndOriginAndHsCodeAndBasis(Long importer_id, Long origin_id, Long hsCode, String basis) {
        return tariffRates.findByImporterIdAndOriginIdAndHsCodeAndBasis(importer_id, origin_id, hsCode, basis).map(tariffRate -> {
            return tariffRate;
        }).orElseThrow(() -> new TariffRateNotFoundException());
    }


    public BigDecimal calculateTariffRate(TariffRateRequestDto rq) {
        TariffRate tariffRateMFN = getTariffRateByImporterAndOriginAndHsCodeAndBasis(rq.getImporter_id(), rq.getOrigin_id(), rq.getHsCode(), "MFN");
        TariffRate tariffRatePref = getTariffRateByImporterAndOriginAndHsCodeAndBasis(rq.getImporter_id(), rq.getOrigin_id(), rq.getHsCode(), "PREF");
        // BigDecimal RVCdefined = findAgreementById(tariffRateMFN.getAgreementId()).getRVC(); // need to add to schema
        BigDecimal RVCdefined = new BigDecimal("40.0"); // temporarily default to 40
        BigDecimal RVC = rq.getMaterialCost()
                        .add(rq.getLabourCost())
                        .add(rq.getOverheadCost())
                        .add(rq.getProfit())
                        .add(rq.getOtherCosts())
                        .divide(rq.getFOB(), 6, RoundingMode.HALF_UP); // will follow order of operations (a + b + c) / d

        TariffRate appliedTariffRate = RVC.compareTo(RVCdefined) >= 0 ? tariffRatePref : tariffRateMFN; // apply pref if rvc >= defined
        BigDecimal totalValue = rq.getTotalValue();
        BigDecimal totalTariff = new BigDecimal(0);
        if (appliedTariffRate.getRateType() == "ad_valorem") {
            BigDecimal avRate = appliedTariffRate.getAdValoremRate();
            totalTariff = totalValue.multiply(avRate);
        }
        else if (appliedTariffRate.getRateType() == "specific") {
            BigDecimal specificAmount = appliedTariffRate.getSpecificAmount();
            // BigDecimal specificunit = appliedTariffRate.getSpecificUnit();
            // tbc
        }
        else { // compound
            BigDecimal avRate = appliedTariffRate.getAdValoremRate();
            BigDecimal specificAmount = appliedTariffRate.getSpecificAmount();
            // BigDecimal specificunit = appliedTariffRate.getSpecificUnit();
            totalTariff = totalValue.multiply(avRate);
            // tbc
        }
        return totalTariff;
    }
}