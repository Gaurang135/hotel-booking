package com.hotelbooking.api;

import com.hotelbooking.api.dto.AddPropertyRequest;
import com.hotelbooking.api.dto.CreateOwnerRequest;
import com.hotelbooking.api.dto.OwnerResponse;
import com.hotelbooking.api.dto.PropertyResponse;
import com.hotelbooking.api.dto.SearchRequest;
import com.hotelbooking.service.PropertyService;
import com.hotelbooking.service.SearchService;
import com.hotelbooking.service.search.SearchResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Properties", description = "Onboard owners and properties, and search for available rooms")
public class PropertyController {

    private final PropertyService propertyService;
    private final SearchService searchService;

    public PropertyController(PropertyService propertyService, SearchService searchService) {
        this.propertyService = propertyService;
        this.searchService = searchService;
    }

    @Operation(summary = "Create an owner account (standalone or chain)")
    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse createOwner(@Valid @RequestBody CreateOwnerRequest request) {
        return OwnerResponse.from(propertyService.addOwner(request.toOwner()));
    }

    @Operation(summary = "Onboard a property under an owner")
    @PostMapping("/owners/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse addProperty(@PathVariable String ownerId, @Valid @RequestBody AddPropertyRequest request) {
        return PropertyResponse.from(propertyService.addProperty(request.toProperty(ownerId)));
    }

    @Operation(summary = "List an owner's properties")
    @GetMapping("/owners/{ownerId}/properties")
    public List<PropertyResponse> propertiesOf(@PathVariable String ownerId) {
        return propertyService.propertiesOf(ownerId).stream().map(PropertyResponse::from).toList();
    }

    @Operation(summary = "Get a property")
    @GetMapping("/properties/{propertyId}")
    public PropertyResponse getProperty(@PathVariable String propertyId) {
        return PropertyResponse.from(propertyService.getProperty(propertyId));
    }

    @Operation(summary = "Search properties with rooms available for the given dates")
    @GetMapping("/properties/search")
    public List<SearchResult> search(@ParameterObject @Valid SearchRequest request) {
        return searchService.search(request.toCriteria());
    }
}
