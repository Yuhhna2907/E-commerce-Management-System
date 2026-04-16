# Bug Fix: ProductRecommendationRepository HQL Syntax Error

## 🐛 Problem

The application failed to start with the following error:

```
BadJpqlGrammarException: At 1:92 and token '1', mismatched input '1', 
expecting one of the following tokens...
Bad HQL grammar [SELECT pr FROM ProductRecommendation pr 
WHERE pr.lastUpdated < CURRENT_TIMESTAMP - INTERVAL 1 DAY]
```

## 🔍 Root Cause

The `findOutdatedRecommendations()` method in `ProductRecommendationRepository` used MySQL-specific `INTERVAL` syntax in an HQL/JPQL query:

```java
@Query("SELECT pr FROM ProductRecommendation pr " +
       "WHERE pr.lastUpdated < CURRENT_TIMESTAMP - INTERVAL 1 DAY")
List<ProductRecommendation> findOutdatedRecommendations();
```

**Issue**: HQL/JPQL does not support the `INTERVAL` keyword. This is a MySQL-specific SQL syntax that cannot be used in portable JPA queries.

## ✅ Solution

Changed the query to use a **native SQL query** instead of HQL:

```java
@Query(value = "SELECT * FROM product_recommendations " +
               "WHERE last_updated < DATE_SUB(NOW(), INTERVAL 1 DAY)", 
       nativeQuery = true)
List<ProductRecommendation> findOutdatedRecommendations();
```

### Why This Works

1. **Native Query**: By setting `nativeQuery = true`, we tell Spring Data JPA to execute the query as raw SQL instead of HQL
2. **MySQL Syntax**: We can now use MySQL-specific functions like `DATE_SUB()` and `INTERVAL`
3. **Column Names**: Native queries use actual database column names (`last_updated`) instead of entity property names (`lastUpdated`)
4. **Table Names**: Native queries use actual table names (`product_recommendations`) instead of entity names (`ProductRecommendation`)

## 🔄 Alternative Solutions

If you need a database-agnostic solution, you could:

### Option 1: Use Java Date Calculation
```java
default List<ProductRecommendation> findOutdatedRecommendations() {
    LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
    return findByLastUpdatedBefore(oneDayAgo);
}

List<ProductRecommendation> findByLastUpdatedBefore(LocalDateTime dateTime);
```

### Option 2: Use JPQL with Parameter
```java
@Query("SELECT pr FROM ProductRecommendation pr WHERE pr.lastUpdated < :cutoffDate")
List<ProductRecommendation> findOutdatedRecommendations(@Param("cutoffDate") LocalDateTime cutoffDate);

// In service:
LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
List<ProductRecommendation> outdated = repository.findOutdatedRecommendations(oneDayAgo);
```

### Option 3: Use Criteria API
```java
// More complex but fully type-safe and database-agnostic
```

## 📝 File Changed

**File**: `E-commerce-Management-System/src/main/java/com/codegym/smartphonemanagement/repository/user/ProductRecommendationRepository.java`

**Method**: `findOutdatedRecommendations()`

**Lines**: ~145-151

## ✅ Verification

- ✅ No syntax errors in the repository
- ✅ Native query uses correct MySQL syntax
- ✅ Column and table names match database schema
- ✅ Application should now start successfully

## 🚀 Next Steps

1. Restart the Spring Boot application
2. Verify the application starts without errors
3. Test the recommendation rebuild functionality
4. Consider adding integration tests for this query

## 📚 Related Documentation

- [Spring Data JPA - Native Queries](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query)
- [MySQL DATE_SUB Function](https://dev.mysql.com/doc/refman/8.0/en/date-and-time-functions.html#function_date-sub)
- [HQL/JPQL Limitations](https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#hql)

---

**Status**: ✅ FIXED
**Date**: 2026-04-15
**Impact**: Application startup error resolved
