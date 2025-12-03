package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.Signa;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.SignaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SignaService {

    private final SignaRepository signaRepository;

    private final AppRepository appRepository;

    public SignaService(SignaRepository signaRepository,
                        AppRepository appRepository) {
        this.signaRepository = signaRepository;
        this.appRepository = appRepository;
    }

    public Signa save(Long appId,Signa signa, String email) {
        App app = appRepository.findById(appId).orElseThrow();
        signa.setApp(app);
        signa.setEmail(email);
        return signaRepository.save(signa);
    }

    public Signa get(Long id) {
        return signaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Signa", "id", id));
    }

    public void delete(Long signaId) {
        signaRepository.deleteById(signaId);
    }

    public Page<Signa> getSignaList(Long appId, Pageable pageable) {
        return signaRepository.findByAppId(appId, pageable);
    }
}
