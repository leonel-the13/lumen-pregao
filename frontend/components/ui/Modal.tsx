"use client";
import { useEffect, useRef, useState, type ReactNode } from "react";
import { Button } from "./Button";
import { Field, Input } from "./Field";

/** Modal acessível (dialog nativo: foco preso, Esc fecha). */
export function Modal({ open, onClose, title, children, actions, labelledBy = "modal-title" }:
  { open: boolean; onClose: () => void; title: string; children?: ReactNode; actions?: ReactNode; labelledBy?: string }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const d = ref.current;
    if (!d) return;
    if (open && !d.open) d.showModal();
    if (!open && d.open) d.close();
  }, [open]);
  return (
    <dialog ref={ref} className="modal-dialog" aria-labelledby={labelledBy} onClose={onClose} onCancel={onClose}>
      <div className="modal stack" style={{ gap: 16 }}>
        <h2 id={labelledBy} className="h2">{title}</h2>
        {children && <div className="muted" style={{ fontSize: 14.5 }}>{children}</div>}
        {actions && <div className="row" style={{ justifyContent: "flex-end", gap: 10 }}>{actions}</div>}
      </div>
    </dialog>
  );
}

/** Confirmação de operação crítica. Nas destrutivas, exige escrever a palavra de confirmação. */
export function ConfirmDialog({ open, onCancel, onConfirm, title, body, confirmLabel, destructive, typeToConfirm, busy }:
  { open: boolean; onCancel: () => void; onConfirm: () => void; title: string; body: ReactNode; confirmLabel: string;
    destructive?: boolean; typeToConfirm?: string; busy?: boolean }) {
  const [typed, setTyped] = useState("");
  useEffect(() => { if (!open) setTyped(""); }, [open]);
  const blocked = !!typeToConfirm && typed !== typeToConfirm;
  return (
    <Modal open={open} onClose={onCancel} title={title}
      actions={<>
        <Button variant="out" onClick={onCancel}>Cancelar</Button>
        <Button variant={destructive ? "danger" : "pri"} onClick={onConfirm} disabled={blocked} loading={busy}>{confirmLabel}</Button>
      </>}>
      <div className="stack">
        <div>{body}</div>
        {typeToConfirm && (
          <Field label={`Escreve ${typeToConfirm} para confirmar`}>
            {({ id }) => <Input id={id} value={typed} onChange={(e) => setTyped(e.target.value)} autoComplete="off" />}
          </Field>
        )}
      </div>
    </Modal>
  );
}
