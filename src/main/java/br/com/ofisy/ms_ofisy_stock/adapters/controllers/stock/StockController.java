package br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock;

import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.CreateStockRequestDTO;
import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.StockResponseDTO;
import br.com.ofisy.ms_ofisy_stock.adapters.controllers.stock.dto.UpdateStockRequestDTO;
import br.com.ofisy.ms_ofisy_stock.adapters.presenters.stock.StockPresenter;
import br.com.ofisy.ms_ofisy_stock.application.stock.add.AddStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.add.AddStockUseCase.AddStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.consume.ConsumeStockUseCase.ConsumeStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.create.CreateStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.create.CreateStockUseCase.CreateStockCommand;
import br.com.ofisy.ms_ofisy_stock.application.stock.identifybyid.IdentifyByIdStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.identifybyproduct.IdentifyByProductStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.list.ListStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.update.UpdateStockUseCase;
import br.com.ofisy.ms_ofisy_stock.application.stock.update.UpdateStockUseCase.UpdateStockCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController implements StockApi {

    private final CreateStockUseCase createStockUseCase;
    private final UpdateStockUseCase updateStockUseCase;
    private final AddStockUseCase addStockUseCase;
    private final ConsumeStockUseCase consumeStockUseCase;
    private final ListStockUseCase listStockUseCase;
    private final IdentifyByIdStockUseCase identifyByIdStockUseCase;
    private final IdentifyByProductStockUseCase identifyByProductStockUseCase;

    @GetMapping
    public ResponseEntity<Page<StockResponseDTO>> getAllStocks(
            @ParameterObject @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(listStockUseCase.execute(pageable).map(StockPresenter::present));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockResponseDTO> getStockById(@PathVariable UUID id) {

        return ResponseEntity.ok(StockPresenter.present(identifyByIdStockUseCase.execute(id)));
    }

    @GetMapping("/productName/{productName}")
    public ResponseEntity<StockResponseDTO> getByProductName(@PathVariable String productName) {

        return ResponseEntity.ok(StockPresenter.present(identifyByProductStockUseCase.execute(productName)));
    }

    @PostMapping
    public ResponseEntity<StockResponseDTO> create(@Valid @RequestBody CreateStockRequestDTO request) {

        CreateStockCommand cmd = new CreateStockCommand(
                request.productName(),
                request.description(),
                request.quantity(),
                request.unitPrice(),
                request.category(),
                request.minThreshold()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(StockPresenter.present(createStockUseCase.execute(cmd)));
    }

    @PostMapping("/{id}/add")
    public ResponseEntity<StockResponseDTO> addStock(@PathVariable UUID id, @RequestParam Integer quantity) {

        return ResponseEntity.ok(StockPresenter.present(addStockUseCase.execute(new AddStockCommand(id, quantity))));
    }

    @PostMapping("/{id}/consume")
    public ResponseEntity<StockResponseDTO> consumeStock(@PathVariable UUID id, @RequestParam Integer quantity) {

        return ResponseEntity.ok(StockPresenter.present(consumeStockUseCase.execute(new ConsumeStockCommand(id, quantity))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StockResponseDTO> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateStockRequestDTO request) {

        UpdateStockCommand cmd = new UpdateStockCommand(
                id,
                request.productName(),
                request.description(),
                request.unitPrice(),
                request.category(),
                request.minThreshold()
        );
        return ResponseEntity.ok(StockPresenter.present(updateStockUseCase.execute(cmd)));
    }
}
