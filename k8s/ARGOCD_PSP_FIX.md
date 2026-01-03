# ArgoCD PSP Sync Fix - Quick Reference

## ⚡ Quick Fix Summary

**Problem:** ArgoCD failing with `PodSecurityPolicy not found` error  
**Cause:** PSP removed in Kubernetes 1.25+  
**Solution:** Migrated to Pod Security Standards (PSS)  
**Status:** ✅ FIXED

## 🔧 What Was Changed

```
✅ k8s/base/namespace.yaml           → Added PSS labels
✅ k8s/base/deployment.yaml          → Init container runs as non-root
✅ k8s/overlays/production/          → Removed PSP, kept NetworkPolicy
✅ k8s/argocd/application-staging.yaml → Fixed YAML corruption
```

## 📌 Pod Security Standards Applied

```yaml
# Added to namespace
pod-security.kubernetes.io/enforce: restricted
pod-security.kubernetes.io/audit: restricted
pod-security.kubernetes.io/warn: restricted
```

## 🚀 Deploy Now

```bash
# Commit and push
git add .
git commit -m "fix: migrate PodSecurityPolicy to Pod Security Standards"
git push origin main

# ArgoCD will auto-sync (if enabled)
# Or manually sync:
argocd app sync product-catalog
```

## ✅ Verify

```bash
# Check namespace
kubectl get ns product-catalog -o yaml | grep pod-security

# Check pods
kubectl get pods -n product-catalog

# Check ArgoCD
kubectl get application product-catalog -n argocd
```

## 📖 Full Documentation

- [ARGOCD_SYNC_FIX_SUMMARY.md](../ARGOCD_SYNC_FIX_SUMMARY.md) - Complete resolution details
- [POD_SECURITY_MIGRATION.md](./POD_SECURITY_MIGRATION.md) - Migration guide

---
**Last Updated:** 2026-01-04  
**Status:** Ready for deployment

