# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.0.4   | :white_check_mark: |
| < 0.0.4 | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in EventBlock, please report it responsibly.

**Do not** open a public issue for security vulnerabilities.

### How to Report

1. Send an email to the project maintainers (see project README for contact information)
2. Include as much detail as possible:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if known)

### Response Timeline

- **Initial response**: Within 48 hours
- **Investigation**: Within 7 days
- **Fix release**: Based on severity, typically within 14 days

### Severity Classification

- **Critical**: Remote code execution, data breach, or complete system compromise
- **High**: Privilege escalation, SQL injection, or significant data exposure
- **Medium**: XSS, CSRF, or moderate data exposure
- **Low**: Minor information disclosure or denial of service

### Disclosure Policy

Once a fix is released, we will publish a security advisory in the CHANGELOG and GitHub Security Advisories.

## Security Best Practices

For Users:
- Keep the plugin updated to the latest version
- Use strong database passwords
- Limit plugin permissions to only what's needed
- Regularly backup your data

For Developers:
- Follow secure coding practices
- Use parameterized queries for database operations
- Validate all user input
- Keep dependencies updated
