package com.codegym.smartphonemanagement.service.category;

import com.codegym.smartphonemanagement.model.Category;
import com.codegym.smartphonemanagement.repository.seller.CategorySellerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService pagination functionality
 * Tests Task 4: Implement pagination support
 * - 4.1: Add paginated findAll method to ICategoryService
 * - 4.2: Implement paginated findAll in CategoryService
 * - 4.3: Keep non-paginated findAll for backward compatibility
 */
@ExtendWith(MockitoExtension.class)
class CategoryServicePaginationTest {
    
    @Mock
    private CategorySellerRepository categoryRepository;
    
    @InjectMocks
    private CategoryService categoryService;
    
    private List<Category> testCategories;
    
    @BeforeEach
    void setUp() {
        // Create test categories
        testCategories = Arrays.asList(
            Category.builder().id(1L).name("Electronics").active(true).build(),
            Category.builder().id(2L).name("Clothing").active(true).build(),
            Category.builder().id(3L).name("Books").active(false).build(),
            Category.builder().id(4L).name("Sports").active(true).build(),
            Category.builder().id(5L).name("Home & Garden").active(true).build()
        );
    }
    
    @Test
    void testFindAll_WithoutPagination_ReturnsAllCategories() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(testCategories);
        
        // Act
        List<Category> result = categoryService.findAll();
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("Electronics", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }
    
    @Test
    void testFindAll_WithPagination_ReturnsPagedResults() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2);
        List<Category> pageContent = testCategories.subList(0, 2);
        Page<Category> expectedPage = new PageImpl<>(pageContent, pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertTrue(result.hasNext());
        assertFalse(result.hasPrevious());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_SecondPage() {
        // Arrange
        Pageable pageable = PageRequest.of(1, 2);
        List<Category> pageContent = testCategories.subList(2, 4);
        Page<Category> expectedPage = new PageImpl<>(pageContent, pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertTrue(result.hasNext());
        assertTrue(result.hasPrevious());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_LastPage() {
        // Arrange
        Pageable pageable = PageRequest.of(2, 2);
        List<Category> pageContent = testCategories.subList(4, 5);
        Page<Category> expectedPage = new PageImpl<>(pageContent, pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(2, result.getNumber());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_SortByNameAscending() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5, Sort.by("name").ascending());
        List<Category> sortedCategories = Arrays.asList(
            testCategories.get(2), // Books
            testCategories.get(1), // Clothing
            testCategories.get(0), // Electronics
            testCategories.get(4), // Home & Garden
            testCategories.get(3)  // Sports
        );
        Page<Category> expectedPage = new PageImpl<>(sortedCategories, pageable, sortedCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals("Books", result.getContent().get(0).getName());
        assertEquals("Sports", result.getContent().get(4).getName());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_SortByIdDescending() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5, Sort.by("id").descending());
        List<Category> sortedCategories = Arrays.asList(
            testCategories.get(4), // id=5
            testCategories.get(3), // id=4
            testCategories.get(2), // id=3
            testCategories.get(1), // id=2
            testCategories.get(0)  // id=1
        );
        Page<Category> expectedPage = new PageImpl<>(sortedCategories, pageable, sortedCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5L, result.getContent().get(0).getId());
        assertEquals(1L, result.getContent().get(4).getId());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_SortByActiveStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 5, Sort.by("active").descending());
        List<Category> sortedCategories = Arrays.asList(
            testCategories.get(0), // active=true
            testCategories.get(1), // active=true
            testCategories.get(3), // active=true
            testCategories.get(4), // active=true
            testCategories.get(2)  // active=false
        );
        Page<Category> expectedPage = new PageImpl<>(sortedCategories, pageable, sortedCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertTrue(result.getContent().get(0).getActive());
        assertFalse(result.getContent().get(4).getActive());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_EmptyResult() {
        // Arrange
        Pageable pageable = PageRequest.of(10, 10); // Page beyond available data
        Page<Category> emptyPage = new PageImpl<>(List.of(), pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(0, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(10, result.getNumber());
        assertFalse(result.hasNext());
        assertTrue(result.hasPrevious());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_PageSizeLargerThanTotal() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 100);
        Page<Category> expectedPage = new PageImpl<>(testCategories, pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(5, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertFalse(result.hasNext());
        assertFalse(result.hasPrevious());
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_BackwardCompatibility_BothMethodsWork() {
        // Arrange
        when(categoryRepository.findAll()).thenReturn(testCategories);
        
        Pageable pageable = PageRequest.of(0, 2);
        List<Category> pageContent = testCategories.subList(0, 2);
        Page<Category> expectedPage = new PageImpl<>(pageContent, pageable, testCategories.size());
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        List<Category> listResult = categoryService.findAll();
        Page<Category> pageResult = categoryService.findAll(pageable);
        
        // Assert
        assertNotNull(listResult);
        assertNotNull(pageResult);
        assertEquals(5, listResult.size());
        assertEquals(2, pageResult.getContent().size());
        verify(categoryRepository, times(1)).findAll();
        verify(categoryRepository, times(1)).findAll(pageable);
    }
    
    @Test
    void testFindAll_WithPagination_MetadataCorrectness() {
        // Arrange
        Pageable pageable = PageRequest.of(1, 3);
        List<Category> pageContent = testCategories.subList(3, 5);
        Page<Category> expectedPage = new PageImpl<>(pageContent, pageable, testCategories.size());
        
        when(categoryRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);
        
        // Act
        Page<Category> result = categoryService.findAll(pageable);
        
        // Assert - Verify all metadata fields
        assertNotNull(result);
        assertEquals(2, result.getNumberOfElements()); // Actual elements in this page
        assertEquals(5, result.getTotalElements());    // Total elements across all pages
        assertEquals(2, result.getTotalPages());       // Total pages (5 elements / 3 per page = 2 pages)
        assertEquals(1, result.getNumber());           // Current page number (0-indexed)
        assertEquals(3, result.getSize());             // Page size
        assertFalse(result.hasNext());                 // No next page
        assertTrue(result.hasPrevious());              // Has previous page
        assertTrue(result.isLast());                   // This is the last page
        assertFalse(result.isFirst());                 // This is not the first page
    }
}
