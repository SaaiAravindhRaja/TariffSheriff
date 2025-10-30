# Tests

This folder contains test files for the TariffSheriff frontend application.

## Test Structure

Tests are organized by type:
- **Unit Tests**: Testing individual components and utility functions
- **Integration Tests**: Testing component interactions and API integrations
- **E2E Tests**: End-to-end testing of complete user workflows

## Running Tests

```bash
# Run all tests
npm test

# Run tests in watch mode
npm run test:watch

# Generate coverage report
npm run test:coverage
```

## Test Coverage

We aim for high test coverage across:
- Core business logic (tariff calculations, RVC computation)
- UI components (forms, charts, data displays)
- API service layer (authentication, tariff lookups)
- Error handling and edge cases

## Writing Tests

When adding new features:
1. Write tests for all new components
2. Test both success and error scenarios
3. Mock external API calls
4. Ensure accessibility compliance
5. Test responsive behavior for different screen sizes

## CI/CD Integration

Tests are automatically run by GitHub Actions on every pull request and push to main. All tests must pass before merging.

For more details on testing strategy, see the main [Contributing Guide](../../../CONTRIBUTING.md).
