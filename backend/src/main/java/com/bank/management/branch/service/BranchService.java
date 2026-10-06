package com.bank.management.branch.service;

import com.bank.management.account.repository.BankAccountRepository;
import com.bank.management.branch.dto.BranchRequest;
import com.bank.management.branch.dto.BranchResponse;
import com.bank.management.branch.entity.Branch;
import com.bank.management.branch.exception.BranchConflictException;
import com.bank.management.branch.exception.BranchNotFoundException;
import com.bank.management.branch.repository.BranchRepository;
import com.bank.management.employee.repository.EmployeeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final BankAccountRepository bankAccountRepository;

    public BranchService(
            BranchRepository branchRepository,
            EmployeeRepository employeeRepository,
            BankAccountRepository bankAccountRepository
    ) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    public BranchResponse createBranch(BranchRequest request) {

        String branchId = normalize(request.branchId());
        String branchName = normalize(request.branchName());
        String location = normalize(request.location());

        if (branchRepository.existsByBranchId(branchId)) {
            throw new BranchConflictException(
                    "Branch ID already exists: " + branchId
            );
        }

        Branch branch = new Branch(
                branchId,
                branchName,
                location,
                LocalDateTime.now()
        );

        Branch savedBranch = branchRepository.save(branch);

        return BranchResponse.from(savedBranch);
    }

    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {

        return branchRepository.findAll()
                .stream()
                .map(BranchResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getBranch(Long id) {

        Branch branch = findBranch(id);

        return BranchResponse.from(branch);
    }

    public BranchResponse updateBranch(
            Long id,
            BranchRequest request
    ) {

        Branch branch = findBranch(id);

        String branchId = normalize(request.branchId());
        String branchName = normalize(request.branchName());
        String location = normalize(request.location());

        if (branchRepository.existsByBranchIdAndIdNot(branchId, id)) {
            throw new BranchConflictException(
                    "Branch ID already exists: " + branchId
            );
        }

        branch.setBranchId(branchId);
        branch.setBranchName(branchName);
        branch.setLocation(location);

        Branch updatedBranch = branchRepository.save(branch);

        return BranchResponse.from(updatedBranch);
    }

    public void deleteBranch(Long id) {

        Branch branch = findBranch(id);

        /*
         * A branch cannot be deleted while employees
         * are still associated with it.
         */
        if (employeeRepository.existsByBranch_Id(branch.getId())) {
            throw new BranchConflictException(
                    "Branch cannot be deleted because employees are associated with it"
            );
        }

        /*
         * A branch cannot be deleted while bank accounts
         * are still associated with it.
         */
        if (bankAccountRepository.existsByBranch_Id(branch.getId())) {
            throw new BranchConflictException(
                    "Branch cannot be deleted because bank accounts are associated with it"
            );
        }

        try {

            branchRepository.delete(branch);

            /*
             * Force the DELETE SQL to execute now so that
             * database foreign-key violations are caught
             * inside this transaction.
             */
            branchRepository.flush();

        } catch (DataIntegrityViolationException exception) {

            throw new BranchConflictException(
                    "Branch cannot be deleted because dependent records are associated with it"
            );
        }
    }

    private Branch findBranch(Long id) {

        return branchRepository.findById(id)
                .orElseThrow(() ->
                        new BranchNotFoundException(id)
                );
    }

    private String normalize(String value) {
        return value.trim();
    }
}